package com.twinkovo.mythLibs.database.query

/**
 * SQL query builder that provides a fluent API for constructing SQL queries.
 * This class helps in building complex SQL queries in a type-safe and readable way.
 * 
 * SQL查询构建器，提供了用于构建SQL查询的流畅API。
 * 此类有助于以类型安全和可读的方式构建复杂的SQL查询。
 */
class QueryBuilder {
    private val select = mutableListOf<String>()
    private val from = mutableListOf<String>()
    private val joins = mutableListOf<String>()
    private val where = mutableListOf<String>()
    private val groupBy = mutableListOf<String>()
    private val having = mutableListOf<String>()
    private val orderBy = mutableListOf<String>()
    private var limit: Int? = null
    private var offset: Int? = null
    private val params = mutableListOf<Any?>()

    /**
     * Adds columns to the SELECT clause.
     * 向SELECT子句添加列。
     *
     * @param columns Column names to select
     *                要选择的列名
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun select(vararg columns: String): QueryBuilder {
        select.addAll(columns)
        return this
    }

    /**
     * Adds tables to the FROM clause.
     * 向FROM子句添加表。
     *
     * @param tables Table names to select from
     *               要从中选择的表名
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun from(vararg tables: String): QueryBuilder {
        from.addAll(tables)
        return this
    }

    /**
     * Adds a JOIN clause with specified condition and type.
     * 添加具有指定条件和类型的JOIN子句。
     *
     * @param table Table name to join
     *              要连接的表名
     * @param condition Join condition
     *                  连接条件
     * @param type Type of join (INNER, LEFT, RIGHT, FULL)
     *             连接类型（INNER、LEFT、RIGHT、FULL）
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun join(table: String, condition: String, type: JoinType = JoinType.INNER): QueryBuilder {
        joins.add("${type.sql} JOIN $table ON $condition")
        return this
    }

    /**
     * Adds a WHERE clause condition.
     * 添加WHERE子句条件。
     *
     * @param condition WHERE condition
     *                  WHERE条件
     * @param parameters Parameters for the condition
     *                   条件的参数
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun where(condition: String, vararg parameters: Any?): QueryBuilder {
        where.add(condition)
        params.addAll(parameters)
        return this
    }

    /**
     * Adds an AND condition to the WHERE clause.
     * 向WHERE子句添加AND条件。
     *
     * @param condition AND condition
     *                  AND条件
     * @param parameters Parameters for the condition
     *                   条件的参数
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun and(condition: String, vararg parameters: Any?): QueryBuilder {
        if (where.isNotEmpty()) {
            where.add("AND $condition")
            params.addAll(parameters)
        } else {
            where(condition, *parameters)
        }
        return this
    }

    /**
     * Adds an OR condition to the WHERE clause.
     * 向WHERE子句添加OR条件。
     *
     * @param condition OR condition
     *                  OR条件
     * @param parameters Parameters for the condition
     *                   条件的参数
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun or(condition: String, vararg parameters: Any?): QueryBuilder {
        if (where.isNotEmpty()) {
            where.add("OR $condition")
            params.addAll(parameters)
        } else {
            where(condition, *parameters)
        }
        return this
    }

    /**
     * Adds columns to the GROUP BY clause.
     * 向GROUP BY子句添加列。
     *
     * @param columns Column names to group by
     *                要分组的列名
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun groupBy(vararg columns: String): QueryBuilder {
        groupBy.addAll(columns)
        return this
    }

    /**
     * Adds a HAVING clause condition.
     * 添加HAVING子句条件。
     *
     * @param condition HAVING condition
     *                  HAVING条件
     * @param parameters Parameters for the condition
     *                   条件的参数
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun having(condition: String, vararg parameters: Any?): QueryBuilder {
        having.add(condition)
        params.addAll(parameters)
        return this
    }

    /**
     * Adds an ORDER BY clause.
     * 添加ORDER BY子句。
     *
     * @param column Column name to order by
     *               要排序的列名
     * @param order Order direction (ASC or DESC)
     *              排序方向（升序或降序）
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun orderBy(column: String, order: OrderType = OrderType.ASC): QueryBuilder {
        orderBy.add("$column ${order.sql}")
        return this
    }

    /**
     * Sets the LIMIT clause.
     * 设置LIMIT子句。
     *
     * @param limit Maximum number of rows to return
     *              要返回的最大行数
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun limit(limit: Int): QueryBuilder {
        this.limit = limit
        return this
    }

    /**
     * Sets the OFFSET clause.
     * 设置OFFSET子句。
     *
     * @param offset Number of rows to skip
     *               要跳过的行数
     * @return This QueryBuilder instance for method chaining
     *         用于方法链式调用的QueryBuilder实例
     */
    fun offset(offset: Int): QueryBuilder {
        this.offset = offset
        return this
    }

    /**
     * Builds the final SQL query and returns it with its parameters.
     * 构建最终的SQL查询并返回它及其参数。
     *
     * @return Query object containing the SQL string and parameters
     *         包含SQL字符串和参数的Query对象
     */
    fun build(): Query {
        val sql = buildString {
            append("SELECT ")
            append(if (select.isEmpty()) "*" else select.joinToString(", "))
            
            append(" FROM ")
            append(from.joinToString(", "))
            
            if (joins.isNotEmpty()) {
                append(" ")
                append(joins.joinToString(" "))
            }
            
            if (where.isNotEmpty()) {
                append(" WHERE ")
                append(where.joinToString(" "))
            }
            
            if (groupBy.isNotEmpty()) {
                append(" GROUP BY ")
                append(groupBy.joinToString(", "))
            }
            
            if (having.isNotEmpty()) {
                append(" HAVING ")
                append(having.joinToString(" AND "))
            }
            
            if (orderBy.isNotEmpty()) {
                append(" ORDER BY ")
                append(orderBy.joinToString(", "))
            }
            
            limit?.let { append(" LIMIT $it") }
            offset?.let { append(" OFFSET $it") }
        }

        return Query(sql, params.toTypedArray())
    }

    /**
     * Resets the query builder to its initial state.
     * 将查询构建器重置为初始状态。
     */
    fun reset() {
        select.clear()
        from.clear()
        joins.clear()
        where.clear()
        groupBy.clear()
        having.clear()
        orderBy.clear()
        limit = null
        offset = null
        params.clear()
    }
}

/**
 * Enumeration of supported JOIN types.
 * 支持的JOIN类型枚举。
 */
enum class JoinType(val sql: String) {
    INNER("INNER"),
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    FULL("FULL")
}

/**
 * Enumeration of supported ORDER types.
 * 支持的排序类型枚举。
 */
enum class OrderType(val sql: String) {
    ASC("ASC"),
    DESC("DESC")
}

/**
 * Data class representing a complete SQL query with its parameters.
 * 表示完整SQL查询及其参数的数据类。
 *
 * @property sql The SQL query string
 *               SQL查询字符串
 * @property parameters Array of parameter values
 *                     参数值数组
 */
data class Query(
    val sql: String,
    val parameters: Array<Any?>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Query

        if (sql != other.sql) return false
        return parameters.contentEquals(other.parameters)
    }

    override fun hashCode(): Int {
        var result = sql.hashCode()
        result = 31 * result + parameters.contentHashCode()
        return result
    }
} 