# Enums serialised to DataStore by name must keep their names, or a rename by R8
# would silently invalidate a user's stored timer state and settings.
-keepclassmembers enum com.jjrapps.aquihaytomate.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
