package com.twinkovo.mythLibs.database.annotations

/**
 * Annotation for marking a property as a database column.
 * This annotation provides detailed configuration for database column mapping.
 * 
 * 用于标记属性作为数据库列的注解。
 * 此注解提供了数据库列映射的详细配置。
 *
 * @property name The name of the column in the database. If empty, the property name will be used.
 *                数据库中的列名。如果为空，将使用属性名。
 * @property type The SQL data type of the column. If empty, it will be automatically inferred from the property type.
 *                列的SQL数据类型。如果为空，将从属性类型自动推断。
 * @property length The length/size of the column (used for types like VARCHAR).
 *                  列的长度/大小（用于VARCHAR等类型）。
 * @property nullable Whether the column can contain NULL values.
 *                    列是否可以包含NULL值。
 * @property primary Whether this column is a primary key.
 *                   此列是否为主键。
 * @property autoIncrement Whether this column auto-increments (typically for primary keys).
 *                        此列是否自动递增（通常用于主键）。
 * @property unique Whether this column must contain unique values.
 *                  此列是否必须包含唯一值。
 * @property default The default value for this column when no value is specified.
 *                   当未指定值时此列的默认值。
 * @property comment Documentation comment for this column.
 *                    此列的文档注释。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val type: String = "",
    val length: Int = 255,
    val nullable: Boolean = false,
    val primary: Boolean = false,
    val autoIncrement: Boolean = false,
    val unique: Boolean = false,
    val default: String = "",
    val comment: String = ""
) 