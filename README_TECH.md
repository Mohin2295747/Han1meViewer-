# Han1meViewer Technical Documentation

/*
 * Copying is the ladder of programmer progress.
 */

// ========== ARCHITECTURE OVERVIEW ========== //
@Architecture: MVVM
@UI: Material 3
@Components: Jetpack (no Compose)
@Network: Retrofit
@ImageLoading: Coil
@VideoPlayer: Jiaozi
@JSON: Kotlin Serialization
@Dialogs: Xpopup
@StateManagement: Flow (no LiveData)

// ========== TARGET AUDIENCE ========== //
/**
 * 1. Android beginners wanting to learn from this project
 * 2. Developers interested in specific feature implementations
 * 3. Anyone who wants to critique/discuss the implementation
 */

// ========== FEATURE BREAKDOWN ========== //

// ----- FEATURE: Resumable Downloads ----- //
/**
 * Key Learnings:
 * 1. WorkManager implementation
 * 2. RecyclerView + DiffUtil with payload optimization
 * 3. Room DB callbacks
 */

// Key Files:
@Worker: HanimeDownloadWorker.kt
@Entity: HanimeDownloadEntity.kt
@DAO: HanimeDownloadDao.kt
@Database: DownloadDatabase.kt
@Adapter: HanimeDownloadingRvAdapter.kt

// Implementation Notes:
/*
 * - Uses 500ms update interval (RESPONSE_INTERVAL constant)
 * - Bitmask pattern for efficient payload handling:
 *   private const val DOWNLOADING = 1        // 0000 0001
 *   private const val PAUSE = 1 shl 1        // 0000 0010
 * - Efficient DB updates with Flow
 */

// ----- FEATURE: CI Update Channels ----- //
/**
 * Key Learnings:
 * 1. GitHub CI/CD pipeline setup
 * 2. Multiple release channels (stable/dev)
 * 3. Automatic build distribution
 */

// Implementation Notes:
/*
 * - Inspired by FooIbar/EhViewer
 * - Two channels:
 *   1. Stable: Manual releases
 *   2. Dev: Auto-built from main branch
 */

// ----- FEATURE: Shared Keyframes ----- //
/**
 * Key Learnings:
 * 1. Kotlin collection operations (groupBy, flatMap, sortedWith)
 * 2. Multi-type RecyclerView adapters
 * 3. Assets file processing
 */

// Key Files:
@Assets: /assets/h_keyframes/
@Repo: DatabaseRepo.kt
@Adapter: SharedHKeyframesRvAdapter.kt
@Entity: HKeyframeEntity.kt

// Data Processing Flow:
/*
 * 1. List JSON files from assets
 * 2. Filter and parse JSONs
 * 3. Sort by group then episode
 * 4. Group by series
 * 5. Flatten into header + items structure
 */

// Multi-type Interface:
interface HKeyframeType : MultiItemEntity {
    companion object {
        const val H_KEYFRAME = 0
        const val HEADER = 1
    }
}

// ========== KEY IMPLEMENTATION SNIPPETS ========== //

// Download Progress Update:
if (System.currentTimeMillis() - delayTime > RESPONSE_INTERVAL) {
    val progress = entity.downloadedLength * 100 / entity.length
    setProgress(workDataOf(PROGRESS to progress.toInt()))
    setForeground(createForegroundInfo(progress.toInt()))
    DatabaseRepo.HanimeDownload.update(entity)
    delayTime = System.currentTimeMillis()
}

// Keyframe Loading:
assets.list("h_keyframes")?.asSequence()
    ?.filter { it.endsWith(".json") }
    ?.mapNotNull { fileName -> 
        assets.open("h_keyframes/$fileName").use { 
            Json.decodeFromStream<HKeyframeEntity>(it) 
        }
    }
    ?.sortedWith(compareBy<HKeyframeEntity> { it.group }.thenBy { it.episode })
    ?.groupBy { it.group ?: "???" }
    ?.flatMap { (group, entities) -> 
        listOf(HKeyframeHeader(title = group, attached = entities)) + entities
    }
