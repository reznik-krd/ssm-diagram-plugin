package com.reznik.krd.ssmdiagramplugin.analyzer.expression

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

/**
 * Resolves supported PSI expressions to semantic state or event values.
 */
class ExpressionResolver {

    /**
     * Resolves a supported PSI expression into one or more symbolic values.
     */
    fun resolve(
        expression: PsiExpression
    ): List<ResolvedValue> {

        return when (expression) {

            is PsiReferenceExpression ->
                resolveReference(expression)

            is PsiMethodCallExpression ->
                resolveMethodCall(expression)

            else ->
                emptyList()
        }
    }

    // -------------------------------------------------------------------------
    // Reference
    // -------------------------------------------------------------------------

    private fun resolveReference(
        expression: PsiReferenceExpression
    ): List<ResolvedValue> {

        val resolved =
            expression.resolve()

        return when (resolved) {

            is PsiEnumConstant ->
                listOf(
                    ResolvedValue(
                        name = resolved.name,
                        source = resolved
                    )
                )

            is PsiLocalVariable ->
                resolveLocalVariable(
                    resolved
                )

            is PsiParameter ->
                resolveParameter(
                    resolved
                )

            else ->
                emptyList()
        }
    }

    // -------------------------------------------------------------------------
    // Method calls
    // -------------------------------------------------------------------------

    private fun resolveMethodCall(
        expression: PsiMethodCallExpression
    ): List<ResolvedValue> {

        val methodName =
            expression.methodExpression.referenceName

        return when (methodName) {

            /*
             * CarState.values()
             *
             * Resolve qualifier CarState to PsiClass,
             * then return every enum constant.
             */
            "values" ->
                resolveEnumValues(expression)

            /*
             * Set.of(...)
             * List.of(...)
             *
             * Each argument may itself resolve into
             * one or several values.
             *
             * Therefore:
             *
             * Set.of(CarState.values())
             *
             * becomes all CarState constants.
             */
            "of" ->
                expression.argumentList
                    .expressions
                    .flatMap(::resolve)

            else ->
                emptyList()
        }
    }

    /**
     * Resolves:
     *
     * CarState.values()
     *
     * into every enum constant declared inside CarState.
     */
    private fun resolveEnumValues(
        expression: PsiMethodCallExpression
    ): List<ResolvedValue> {

        val qualifier =
            expression.methodExpression.qualifierExpression
                ?: return emptyList()

        val enumClass =
            resolveClass(
                qualifier
            ) ?: return emptyList()

        if (!enumClass.isEnum) {
            return emptyList()
        }

        return enumClass.fields
            .filterIsInstance<PsiEnumConstant>()
            .map { constant ->
                ResolvedValue(
                    name = constant.name,
                    source = constant
                )
            }
    }

    /**
     * Resolves an expression representing a class.
     *
     * Example:
     *
     * CarState.values()
     * ^^^^^^^^
     */
    private fun resolveClass(
        expression: PsiExpression
    ): PsiClass? {

        return when (expression) {

            is PsiReferenceExpression -> {

                when (
                    val resolved =
                        expression.resolve()
                ) {

                    is PsiClass ->
                        resolved

                    else ->
                        null
                }
            }

            else ->
                null
        }
    }

    // -------------------------------------------------------------------------
    // Variables
    // -------------------------------------------------------------------------

    private fun resolveLocalVariable(
        variable: PsiLocalVariable
    ): List<ResolvedValue> {

        val initializer =
            variable.initializer
                ?: return emptyList()

        return resolve(initializer)
    }

    // -------------------------------------------------------------------------
    // foreach
    // -------------------------------------------------------------------------

    private fun resolveParameter(
        parameter: PsiParameter
    ): List<ResolvedValue> {

        val foreach =
            PsiTreeUtil.getParentOfType(
                parameter,
                PsiForeachStatement::class.java
            ) ?: return emptyList()

        if (
            foreach.iterationParameter !=
            parameter
        ) {
            return emptyList()
        }

        val iteratedValue =
            foreach.iteratedValue
                ?: return emptyList()

        return resolveCollection(
            iteratedValue
        )
    }

    // -------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------

    private fun resolveCollection(
        expression: PsiExpression
    ): List<ResolvedValue> {

        /*
         * First try the generic resolver.
         *
         * This already understands:
         *
         * List.of(...)
         * Set.of(...)
         * CarState.values()
         */
        val directlyResolved =
            resolve(expression)

        if (directlyResolved.isNotEmpty()) {
            return directlyResolved
        }

        /*
         * Collection stored inside a local variable:
         *
         * List<CarState> states = List.of(...);
         *
         * for (CarState state : states) { ... }
         */
        if (
            expression is PsiReferenceExpression
        ) {

            val resolved =
                expression.resolve()

            if (
                resolved is PsiLocalVariable
            ) {

                val initializer =
                    resolved.initializer
                        ?: return emptyList()

                return resolveCollection(
                    initializer
                )
            }
        }

        return emptyList()
    }
}