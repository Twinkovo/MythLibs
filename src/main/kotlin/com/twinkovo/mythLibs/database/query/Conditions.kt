package com.twinkovo.mythLibs.database.query

/**
 * SQL query conditions builder that provides a fluent API for constructing WHERE clauses.
 * This class helps in building complex SQL conditions in a type-safe and readable way.
 * 
 * SQL查询条件构建器，提供了用于构建WHERE子句的流畅API。
 * 此类有助于以类型安全和可读的方式构建复杂的SQL条件。
 */
class Conditions {
    private val conditions = mutableListOf<String>()
    private val parameters = mutableListOf<Any?>()

    /**
     * Adds an equality condition (column = value).
     * 添加等于条件（column = value）。
     */
    infix fun String.eq(value: Any?): Conditions {
        conditions.add("$this = ?")
        parameters.add(value)
        return this@Conditions
    }

    /**
     * Adds an inequality condition (column != value).
     * 添加不等于条件（column != value）。
     */
    infix fun String.ne(value: Any?): Conditions {
        conditions.add("$this != ?")
        parameters.add(value)
        return this@Conditions
    }

    /**
     * Adds a greater than condition (column > value).
     * 添加大于条件（column > value）。
     */
    infix fun String.gt(value: Any): Conditions {
        conditions.add("$this > ?")
        parameters.add(value)
        return this@Conditions
    }

    /**
     * Adds a greater than or equal condition (column >= value).
     * 添加大于等于条件（column >= value）。
     */
    infix fun String.ge(value: Any): Conditions {
        conditions.add("$this >= ?")
        parameters.add(value)
        return this@Conditions
    }

    /**
     * Adds a less than condition (column < value).
     * 添加小于条件（column < value）。
     */
    infix fun String.lt(value: Any): Conditions {
        conditions.add("$this < ?")
        parameters.add(value)
        return this@Conditions
    }

    /**
     * Adds a less than or equal condition (column <= value).
     * 添加小于等于条件（column <= value）。
     */
    infix fun String.le(value: Any): Conditions {
        conditions.add("$this <= ?")
        parameters.add(value)
        return this@Conditions
    }

    /**
     * Adds a LIKE pattern matching condition.
     * 添加LIKE模式匹配条件。
     */
    infix fun String.like(pattern: String): Conditions {
        conditions.add("$this LIKE ?")
        parameters.add(pattern)
        return this@Conditions
    }

    /**
     * Adds an IN condition for multiple possible values.
     * 添加IN条件用于多个可能的值。
     */
    infix fun String.`in`(values: Collection<Any>): Conditions {
        conditions.add("$this IN (${values.joinToString { "?" }})")
        parameters.addAll(values)
        return this@Conditions
    }

    /**
     * Adds a BETWEEN condition for a range of values.
     * 添加BETWEEN条件用于值的范围。
     */
    fun String.between(start: Any, end: Any): Conditions {
        conditions.add("$this BETWEEN ? AND ?")
        parameters.add(start)
        parameters.add(end)
        return this@Conditions
    }

    /**
     * Adds an IS NULL condition.
     * 添加IS NULL条件。
     */
    fun String.isNull(): Conditions {
        conditions.add("$this IS NULL")
        return this@Conditions
    }

    /**
     * Adds an IS NOT NULL condition.
     * 添加IS NOT NULL条件。
     */
    fun String.isNotNull(): Conditions {
        conditions.add("$this IS NOT NULL")
        return this@Conditions
    }

    /**
     * Combines conditions with AND operator.
     * 使用AND运算符组合条件。
     */
    infix fun Conditions.and(condition: Conditions.() -> Unit): Conditions {
        val other = Conditions().apply(condition)
        if (other.conditions.isNotEmpty()) {
            conditions.add("AND (${other.conditions.joinToString(" AND ")})")
            parameters.addAll(other.parameters)
        }
        return this
    }

    /**
     * Combines conditions with OR operator.
     * 使用OR运算符组合条件。
     */
    infix fun Conditions.or(condition: Conditions.() -> Unit): Conditions {
        val other = Conditions().apply(condition)
        if (other.conditions.isNotEmpty()) {
            conditions.add("OR (${other.conditions.joinToString(" AND ")})")
            parameters.addAll(other.parameters)
        }
        return this
    }

    /**
     * Builds the final WHERE clause and returns it with its parameters.
     * 构建最终的WHERE子句并返回它及其参数。
     *
     * @return Pair of the WHERE clause string and array of parameters
     *         WHERE子句字符串和参数数组的配对
     */
    fun build(): Pair<String, Array<Any?>> {
        return if (conditions.isEmpty()) {
            "" to emptyArray()
        } else {
            conditions.joinToString(" ") to parameters.toTypedArray()
        }
    }
} 