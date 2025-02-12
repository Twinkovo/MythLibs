package com.twinkovo.mythLibs.database.model

import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Class representing the result of a database query.
 * Provides methods to access and manipulate query results in various formats.
 * 
 * 表示数据库查询结果的类。
 * 提供以各种格式访问和操作查询结果的方法。
 *
 * @property rows List of result rows, where each row is a map of column names to values
 *                结果行列表，每行是列名到值的映射
 */
class QueryResult(private val rows: List<Map<String, Any?>>) {
    /**
     * Gets the number of rows in the result set.
     * 获取结果集中的行数。
     */
    val size: Int get() = rows.size

    /**
     * Checks if the result set is empty.
     * 检查结果集是否为空。
     */
    val isEmpty: Boolean get() = rows.isEmpty()

    /**
     * Gets the first row of the result set, or null if empty.
     * 获取结果集的第一行，如果为空则返回null。
     *
     * @return Map of column names to values, or null if no rows exist
     *         列名到值的映射，如果没有行则返回null
     */
    fun first(): Map<String, Any?>? = rows.firstOrNull()

    /**
     * Gets all rows in the result set.
     * 获取结果集中的所有行。
     *
     * @return List of maps, where each map represents a row with column names as keys
     *         映射列表，每个映射表示一行，列名作为键
     */
    fun all(): List<Map<String, Any?>> = rows

    /**
     * Maps the result set to a list of objects of the specified class.
     * 将结果集映射为指定类的对象列表。
     *
     * @param clazz Target class to map to
     *              要映射到的目标类
     * @return List of objects of the specified class
     *         指定类的对象列表
     * @throws IllegalArgumentException if the target class doesn't have a primary constructor
     *                                 如果目标类没有主构造函数则抛出异常
     */
    fun <T : Any> map(clazz: KClass<T>): List<T> {
        val constructor = clazz.primaryConstructor ?: throw IllegalArgumentException(
            "Class ${clazz.simpleName} must have a primary constructor"
        )

        return rows.map { row ->
            val params = constructor.parameters.associateWith { param ->
                row[param.name]?.let { value ->
                    convertValue(value, param.type.classifier as KClass<*>)
                }
            }
            constructor.callBy(params)
        }
    }

    /**
     * Maps the first row to an object of the specified class.
     * 将第一行映射为指定类的对象。
     *
     * @param clazz Target class to map to
     *              要映射到的目标类
     * @return Object of the specified class, or null if result set is empty
     *         指定类的对象，如果结果集为空则返回null
     */
    fun <T : Any> mapOne(clazz: KClass<T>): T? = if (isEmpty) null else map(clazz).first()

    /**
     * Gets a list of values from the specified column.
     * 获取指定列的值列表。
     *
     * @param column Name of the column
     *               列名
     * @return List of values from the specified column
     *         指定列的值列表
     */
    fun <T> column(column: String): List<T> {
        @Suppress("UNCHECKED_CAST")
        return rows.mapNotNull { it[column] as? T }
    }

    /**
     * Gets the first value from the specified column.
     * 获取指定列的第一个值。
     *
     * @param column Name of the column
     *               列名
     * @return First value from the specified column, or null if result set is empty
     *         指定列的第一个值，如果结果集为空则返回null
     */
    fun <T> columnOne(column: String): T? = if (isEmpty) null else column<T>(column).first()

    /**
     * Converts a value to the specified target type.
     * 将值转换为指定的目标类型。
     *
     * @param value Value to convert
     *              要转换的值
     * @param targetType Target type class
     *                   目标类型类
     * @return Converted value, or null if conversion is not possible
     *         转换后的值，如果无法转换则返回null
     */
    private fun convertValue(value: Any, targetType: KClass<*>): Any? {
        return when {
            targetType.isInstance(value) -> value
            targetType == Int::class -> value.toString().toIntOrNull()
            targetType == Long::class -> value.toString().toLongOrNull()
            targetType == Double::class -> value.toString().toDoubleOrNull()
            targetType == Float::class -> value.toString().toFloatOrNull()
            targetType == Boolean::class -> value.toString().toBooleanStrictOrNull()
            targetType == String::class -> value.toString()
            else -> null
        }
    }

    companion object {
        /**
         * Creates a QueryResult instance from a JDBC ResultSet.
         * 从JDBC ResultSet创建QueryResult实例。
         *
         * @param rs JDBC ResultSet to convert
         *          要转换的JDBC ResultSet
         * @return QueryResult instance containing the data from the ResultSet
         *         包含ResultSet数据的QueryResult实例
         */
        fun fromResultSet(rs: ResultSet): QueryResult {
            val metaData = rs.metaData
            val columnCount = metaData.columnCount
            val columnNames = (1..columnCount).map { metaData.getColumnName(it) }
            
            val rows = mutableListOf<Map<String, Any?>>()
            while (rs.next()) {
                val row = columnNames.associateWith { rs.getObject(it) }
                rows.add(row)
            }
            
            return QueryResult(rows)
        }
    }
} 