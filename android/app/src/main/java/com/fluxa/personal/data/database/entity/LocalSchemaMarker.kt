package com.fluxa.personal.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room needs an entity before the first financial table is introduced.
@Entity(tableName = "local_schema_marker")
internal data class LocalSchemaMarker(@PrimaryKey val id: Int)
