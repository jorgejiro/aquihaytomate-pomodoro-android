# ─── Enums that are a storage contract ──────────────────────────────────────
#
# SlotType and TimerStatus are persisted BY NAME: in `focus_session.slot_type` and in the timer state
# DataStore. AlertSound, AppLanguage and WidgetBackground carry their own stable `id` strings, but they
# live in the same package and cost nothing to keep.
#
# `<fields>` is the part that matters, and the reason this rule is not the usual
# `-keepclassmembers enum { values(); valueOf(); }` recipe. That one keeps the two methods but lets R8
# rename the constants themselves, so `SlotType.FOCUS.name` returns "e" in a release build. Everything
# looks fine — the build is internally consistent — until the next release assigns different letters and
# every stored row and every saved timer state stops mapping. Verified against
# `app/build/outputs/mapping/release/mapping.txt`, which is where this was caught.
-keep enum com.jjrapps.aquihaytomate.domain.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Room ───────────────────────────────────────────────────────────────────
#
# The entity's field names are the column names, and Room resolves them reflectively in the generated
# adapters. Room ships its own consumer rules, but they do not cover our own entity classes.
-keep class com.jjrapps.aquihaytomate.data.local.db.FocusSessionEntity { *; }
-keep class com.jjrapps.aquihaytomate.data.local.db.DayTotals { *; }

# ─── Widget ─────────────────────────────────────────────────────────────────
#
# The launcher instantiates the provider by name from the manifest, in another process. Losing the class
# name would leave the widget permanently blank with no error anywhere.
-keep class com.jjrapps.aquihaytomate.widget.PomodoroWidgetProvider { *; }
