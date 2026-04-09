# DreasonFrame ProGuard Rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class com.dreason.frame.core.database.entity.** { *; }

# Keep model classes (used by Room converters)
-keep class com.dreason.frame.core.model.** { *; }
