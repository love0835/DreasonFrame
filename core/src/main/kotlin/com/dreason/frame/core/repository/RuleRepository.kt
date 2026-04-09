package com.dreason.frame.core.repository

import android.content.Context
import com.dreason.frame.core.database.dao.RuleDao
import com.dreason.frame.core.database.entity.RuleEntity
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.RoutingRule
import com.dreason.frame.core.model.RuleType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepository @Inject constructor(
    private val ruleDao: RuleDao,
    @ApplicationContext private val context: Context,
) {

    fun getAll(): Flow<List<RoutingRule>> =
        ruleDao.getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getById(id: Long): RoutingRule? =
        ruleDao.getById(id)?.toDomain()

    suspend fun getAllEnabled(): List<RoutingRule> =
        ruleDao.getAllEnabled().map { it.toDomain() }

    suspend fun getEnabledByType(type: RuleType): List<RoutingRule> =
        ruleDao.getEnabledByType(type.name).map { it.toDomain() }

    fun getByGroupTag(groupTag: String): Flow<List<RoutingRule>> =
        ruleDao.getByGroupTag(groupTag).map { entities -> entities.map { it.toDomain() } }

    suspend fun insert(rule: RoutingRule): Long =
        ruleDao.insert(rule.toEntity())

    suspend fun insertAll(rules: List<RoutingRule>) =
        ruleDao.insertAll(rules.map { it.toEntity() })

    suspend fun update(rule: RoutingRule) =
        ruleDao.update(rule.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteById(id: Long) =
        ruleDao.deleteById(id)

    suspend fun deleteAllBuiltIn() =
        ruleDao.deleteAllBuiltIn()

    suspend fun count(): Int = ruleDao.count()

    suspend fun importBuiltInRules() {
        ruleDao.deleteAllBuiltIn()

        val rules = mutableListOf<RuleEntity>()

        // Import China domain rules
        loadAssetLines("china-domains.txt").forEach { domain ->
            rules.add(RuleEntity(
                type = RuleType.DOMAIN_SUFFIX.name,
                pattern = domain,
                route = Route.CHINA_PROXY.name,
                priority = 100,
                isBuiltIn = true,
                groupTag = "china-domains",
            ))
        }

        // Import Taiwan domain rules
        loadAssetLines("taiwan-domains.txt").forEach { domain ->
            rules.add(RuleEntity(
                type = RuleType.DOMAIN_SUFFIX.name,
                pattern = domain,
                route = Route.TAIWAN_PROXY.name,
                priority = 100,
                isBuiltIn = true,
                groupTag = "taiwan-domains",
            ))
        }

        // Import China CIDR rules
        loadAssetLines("china-cidr.txt").forEach { cidr ->
            rules.add(RuleEntity(
                type = RuleType.IP_CIDR.name,
                pattern = cidr,
                route = Route.CHINA_PROXY.name,
                priority = 50,
                isBuiltIn = true,
                groupTag = "china-cidr",
            ))
        }

        // Import Taiwan CIDR rules
        loadAssetLines("taiwan-cidr.txt").forEach { cidr ->
            rules.add(RuleEntity(
                type = RuleType.IP_CIDR.name,
                pattern = cidr,
                route = Route.TAIWAN_PROXY.name,
                priority = 50,
                isBuiltIn = true,
                groupTag = "taiwan-cidr",
            ))
        }

        ruleDao.insertAll(rules)
    }

    private fun loadAssetLines(fileName: String): List<String> =
        try {
            context.assets.open(fileName).bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { it.trim() }
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }

    private fun RuleEntity.toDomain() = RoutingRule(
        id = id,
        type = RuleType.valueOf(type),
        pattern = pattern,
        route = Route.valueOf(route),
        priority = priority,
        enabled = enabled,
        isBuiltIn = isBuiltIn,
        groupTag = groupTag,
    )

    private fun RoutingRule.toEntity() = RuleEntity(
        id = id,
        type = type.name,
        pattern = pattern,
        route = route.name,
        priority = priority,
        enabled = enabled,
        isBuiltIn = isBuiltIn,
        groupTag = groupTag,
    )
}
