package com.xiaomo.feishu.tools.bitable

import android.util.Log
import com.xiaomo.feishu.FeishuClient
import com.xiaomo.feishu.FeishuConfig
import com.xiaomo.feishu.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 飞书多维表格工具集
 * 对齐 OpenClaw src/bitable-tools
 */
class FeishuBitableTools(config: FeishuConfig, client: FeishuClient) {
    private val createTool = BitableCreateTool(config, client)
    private val readTool = BitableReadTool(config, client)
    private val updateTool = BitableUpdateTool(config, client)
    private val deleteTool = BitableDeleteTool(config, client)
    private val queryTool = BitableQueryTool(config, client)

    fun getAllTools(): List<FeishuToolBase> {
        return listOf(createTool, readTool, updateTool, deleteTool, queryTool)
    }

    fun getToolDefinitions(): List<ToolDefinition> {
        return getAllTools().filter { it.isEnabled() }.map { it.getToolDefinition() }
    }
}

/**
 * 创建记录工具
 */
class BitableCreateTool(config: FeishuConfig, client: FeishuClient) : FeishuToolBase(config, client) {
    override val name = "feishu_bitable_create"
    override val description = "在飞书多维表格中创建记录"

    override fun isEnabled() = config.enableBitableTools

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        try {
            val appToken = args["app_token"] as? String ?: return@withContext ToolResult.error("Missing app_token")
            val tableId = args["table_id"] as? String ?: return@withContext ToolResult.error("Missing table_id")
            @Suppress("UNCHECKED_CAST")
            val fields = args["fields"] as? Map<String, Any?> ?: return@withContext ToolResult.error("Missing fields")

            val body = mapOf("fields" to fields)

            val result = client.post("/open-apis/bitable/v1/apps/$appToken/tables/$tableId/records", body)

            if (result.isFailure) {
                return@withContext ToolResult.error(result.exceptionOrNull()?.message ?: "Failed")
            }

            val data = result.getOrNull()?.getAsJsonObject("data")
            val record = data?.getAsJsonObject("record")
            val recordId = record?.get("record_id")?.asString
                ?: return@withContext ToolResult.error("Missing record_id")

            Log.d("BitableCreateTool", "Record created: $recordId")
            ToolResult.success(mapOf(
                "record_id" to recordId,
                "app_token" to appToken,
                "table_id" to tableId
            ))

        } catch (e: Exception) {
            Log.e("BitableCreateTool", "Failed", e)
            ToolResult.error(e.message ?: "Unknown error")
        }
    }

    override fun getToolDefinition() = ToolDefinition(
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = ParametersSchema(
                properties = mapOf(
                    "app_token" to PropertySchema("string", "多维表格appToken"),
                    "table_id" to PropertySchema("string", "数据表ID"),
                    "fields" to PropertySchema("object", "字段值对象，如 {\"字段1\": \"值1\", \"字段2\": \"值2\"}")
                ),
                required = listOf("app_token", "table_id", "fields")
            )
        )
    )
}

/**
 * 读取记录工具
 */
class BitableReadTool(config: FeishuConfig, client: FeishuClient) : FeishuToolBase(config, client) {
    override val name = "feishu_bitable_read"
    override val description = "读取飞书多维表格记录"

    override fun isEnabled() = config.enableBitableTools

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        try {
            val appToken = args["app_token"] as? String ?: return@withContext ToolResult.error("Missing app_token")
            val tableId = args["table_id"] as? String ?: return@withContext ToolResult.error("Missing table_id")
            val recordId = args["record_id"] as? String ?: return@withContext ToolResult.error("Missing record_id")

            val result = client.get("/open-apis/bitable/v1/apps/$appToken/tables/$tableId/records/$recordId")

            if (result.isFailure) {
                return@withContext ToolResult.error(result.exceptionOrNull()?.message ?: "Failed")
            }

            val data = result.getOrNull()?.getAsJsonObject("data")
            val record = data?.getAsJsonObject("record")
            val fields = record?.getAsJsonObject("fields")

            Log.d("BitableReadTool", "Record read: $recordId")
            ToolResult.success(mapOf(
                "record_id" to recordId,
                "fields" to (fields?.toString() ?: "{}")
            ))

        } catch (e: Exception) {
            Log.e("BitableReadTool", "Failed", e)
            ToolResult.error(e.message ?: "Unknown error")
        }
    }

    override fun getToolDefinition() = ToolDefinition(
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = ParametersSchema(
                properties = mapOf(
                    "app_token" to PropertySchema("string", "多维表格appToken"),
                    "table_id" to PropertySchema("string", "数据表ID"),
                    "record_id" to PropertySchema("string", "记录ID")
                ),
                required = listOf("app_token", "table_id", "record_id")
            )
        )
    )
}

/**
 * 更新记录工具
 */
class BitableUpdateTool(config: FeishuConfig, client: FeishuClient) : FeishuToolBase(config, client) {
    override val name = "feishu_bitable_update"
    override val description = "更新飞书多维表格记录"

    override fun isEnabled() = config.enableBitableTools

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        try {
            val appToken = args["app_token"] as? String ?: return@withContext ToolResult.error("Missing app_token")
            val tableId = args["table_id"] as? String ?: return@withContext ToolResult.error("Missing table_id")
            val recordId = args["record_id"] as? String ?: return@withContext ToolResult.error("Missing record_id")
            @Suppress("UNCHECKED_CAST")
            val fields = args["fields"] as? Map<String, Any?> ?: return@withContext ToolResult.error("Missing fields")

            val body = mapOf("fields" to fields)

            val result = client.put("/open-apis/bitable/v1/apps/$appToken/tables/$tableId/records/$recordId", body)

            if (result.isFailure) {
                return@withContext ToolResult.error(result.exceptionOrNull()?.message ?: "Failed")
            }

            Log.d("BitableUpdateTool", "Record updated: $recordId")
            ToolResult.success(mapOf("record_id" to recordId))

        } catch (e: Exception) {
            Log.e("BitableUpdateTool", "Failed", e)
            ToolResult.error(e.message ?: "Unknown error")
        }
    }

    override fun getToolDefinition() = ToolDefinition(
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = ParametersSchema(
                properties = mapOf(
                    "app_token" to PropertySchema("string", "多维表格appToken"),
                    "table_id" to PropertySchema("string", "数据表ID"),
                    "record_id" to PropertySchema("string", "记录ID"),
                    "fields" to PropertySchema("object", "要更新的字段值对象")
                ),
                required = listOf("app_token", "table_id", "record_id", "fields")
            )
        )
    )
}

/**
 * 删除记录工具
 */
class BitableDeleteTool(config: FeishuConfig, client: FeishuClient) : FeishuToolBase(config, client) {
    override val name = "feishu_bitable_delete"
    override val description = "删除飞书多维表格记录"

    override fun isEnabled() = config.enableBitableTools

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        try {
            val appToken = args["app_token"] as? String ?: return@withContext ToolResult.error("Missing app_token")
            val tableId = args["table_id"] as? String ?: return@withContext ToolResult.error("Missing table_id")
            val recordId = args["record_id"] as? String ?: return@withContext ToolResult.error("Missing record_id")

            val result = client.delete("/open-apis/bitable/v1/apps/$appToken/tables/$tableId/records/$recordId")

            if (result.isFailure) {
                return@withContext ToolResult.error(result.exceptionOrNull()?.message ?: "Failed")
            }

            Log.d("BitableDeleteTool", "Record deleted: $recordId")
            ToolResult.success(mapOf("record_id" to recordId))

        } catch (e: Exception) {
            Log.e("BitableDeleteTool", "Failed", e)
            ToolResult.error(e.message ?: "Unknown error")
        }
    }

    override fun getToolDefinition() = ToolDefinition(
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = ParametersSchema(
                properties = mapOf(
                    "app_token" to PropertySchema("string", "多维表格appToken"),
                    "table_id" to PropertySchema("string", "数据表ID"),
                    "record_id" to PropertySchema("string", "记录ID")
                ),
                required = listOf("app_token", "table_id", "record_id")
            )
        )
    )
}

/**
 * 查询记录工具
 */
class BitableQueryTool(config: FeishuConfig, client: FeishuClient) : FeishuToolBase(config, client) {
    override val name = "feishu_bitable_query"
    override val description = "查询飞书多维表格记录"

    override fun isEnabled() = config.enableBitableTools

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        try {
            val appToken = args["app_token"] as? String ?: return@withContext ToolResult.error("Missing app_token")
            val tableId = args["table_id"] as? String ?: return@withContext ToolResult.error("Missing table_id")
            val filter = args["filter"] as? String
            val pageSize = (args["page_size"] as? Number)?.toInt() ?: 100

            val params = mutableMapOf("page_size" to pageSize.toString())
            if (filter != null) {
                params["filter"] = filter
            }

            val path = "/open-apis/bitable/v1/apps/$appToken/tables/$tableId/records?" +
                    params.entries.joinToString("&") { "${it.key}=${it.value}" }
            val result = client.get(path)

            if (result.isFailure) {
                return@withContext ToolResult.error(result.exceptionOrNull()?.message ?: "Failed")
            }

            val data = result.getOrNull()?.getAsJsonObject("data")
            val items = data?.getAsJsonArray("items") ?: return@withContext ToolResult.success(mapOf("records" to emptyList<Map<String, Any>>()))

            val records = items.map { item ->
                val obj = item.asJsonObject
                mapOf(
                    "record_id" to (obj.get("record_id")?.asString ?: ""),
                    "fields" to (obj.getAsJsonObject("fields")?.toString() ?: "{}")
                )
            }

            Log.d("BitableQueryTool", "Records queried: ${records.size}")
            ToolResult.success(mapOf(
                "app_token" to appToken,
                "table_id" to tableId,
                "records" to records
            ))

        } catch (e: Exception) {
            Log.e("BitableQueryTool", "Failed", e)
            ToolResult.error(e.message ?: "Unknown error")
        }
    }

    override fun getToolDefinition() = ToolDefinition(
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = ParametersSchema(
                properties = mapOf(
                    "app_token" to PropertySchema("string", "多维表格appToken"),
                    "table_id" to PropertySchema("string", "数据表ID"),
                    "filter" to PropertySchema("string", "筛选条件（可选）"),
                    "page_size" to PropertySchema("number", "每页数量（默认100）")
                ),
                required = listOf("app_token", "table_id")
            )
        )
    )
}
