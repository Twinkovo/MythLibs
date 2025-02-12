package com.twinkovo.mythLibs.database.annotations

/**
 * Annotation for marking a class as a database table.
 * This annotation provides configuration for database table mapping and properties.
 * 
 * 用于标记类作为数据库表的注解。
 * 此注解提供了数据库表映射和属性的配置。
 *
 * @property name The name of the table in the database.
 *                数据库中的表名。
 * @property engine The database engine to use (MySQL specific, e.g., InnoDB, MyISAM).
 *                  要使用的数据库引擎（MySQL特有，如InnoDB、MyISAM）。
 * @property charset The character set to use for the table (MySQL specific).
 *                   表使用的字符集（MySQL特有）。
 * @property comment Documentation comment for the table.
 *                    表的文档注释。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(
    val name: String,
    val engine: String = "InnoDB",
    val charset: String = "utf8mb4",
    val comment: String = ""
) 