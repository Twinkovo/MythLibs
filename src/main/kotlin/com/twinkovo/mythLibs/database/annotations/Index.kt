package com.twinkovo.mythLibs.database.annotations

/**
 * Annotation for marking a property as a database index.
 * This annotation is used to define database indexing strategies for improved query performance.
 * 
 * 用于标记属性作为数据库索引的注解。
 * 此注解用于定义数据库索引策略以提高查询性能。
 *
 * @property name The name of the index. If empty, a name will be automatically generated.
 *                索引的名称。如果为空，将自动生成一个名称。
 * @property type The type of index (e.g., BTREE, HASH). Different databases support different index types.
 *                索引的类型（如BTREE、HASH）。不同的数据库支持不同的索引类型。
 * @property unique Whether this index enforces unique values.
 *                  此索引是否强制唯一值。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Index(
    val name: String = "",
    val type: String = "BTREE",
    val unique: Boolean = false
) 