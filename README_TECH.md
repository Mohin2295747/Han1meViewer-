# Han1meViewer Technology Related

> Copying is the ladder of programmer progress.

## Summary

This software uses MVVM architecture, Material 3 visual style, Jetpack is definitely used, but Compose is not used (there is a saying that Compose is not used
I was really tired of writing XML. I used Retrofit for network requests, Coil for image loading, Jiaozi for video playback, and Json parsing.
Serialization, Xpopup is used for some pop-ups. LiveData is not used, and all are replaced by the more powerful Flow.

## Target Audience

Who is this article mainly for? First, those who are new to Android and want to see how this project is written, or are very interested in a certain function and want to learn it and quickly integrate it into their own
In the App; secondly, ordinary developers are interested and can come to support me. It would be better if they can learn something. If they write something wrong, they can post a discussion and criticize me.

## Functional Analysis

### Resumable download

#### You can learn

1. How do I use WorkManager and perform basic management of download tasks in WorkManager?
2. When using RecyclerView and DiffUtil, how can we make full use of the `payload` parameter to refresh a specific control?
3. When using Room, how do I implement callbacks through the database?

#### Key Files

- [HanimeDownloadWorker.kt](app/src/main/java/com/yenaly/han1meviewer/worker/HanimeDownloadWorker.kt) - Key job class
- [HanimeDownloadEntity.kt](app/src/main/java/com/yenaly/han1meviewer/logic/entity/HanimeDownloadEntity.kt) - Download entity class
- [HanimeDownloadDao.kt](app/src/main/java/com/yenaly/han1meviewer/logic/dao/HanimeDownloadDao.kt) - 下载 Dao 类
- [DownloadDatabase.kt](app/src/main/java/com/yenaly/han1meviewer/logic/dao/DownloadDatabase.kt) - Download database class
- [HanimeDownloadingRvAdapter.kt](app/src/main/java/com/yenaly/han1meviewer/ui/adapter/HanimeDownloadingRvAdapter.kt) - 下载界面的 RecyclerView Adapter

#### explain

You may ask me, you have implemented it with just a few files? Where is my interface? How do you call back without an interface?

**First,** read my article [How can a newbie quickly implement simple, save-state, resumable downloads with background downloading? A Jetpack library handles it all! ](https://juejin.cn/post/7278929337067225149), then read the following.

But don't copy it. Pay attention to the following points before using it:

1. Can the download you're trying to download be resumed? For video apps, most videos are resumable, since they need to be played after all! So I didn't have to worry about that much when implementing the download.
2. Do you need to perform very granular operations on each download task? It's not impossible, but it might be a bit cumbersome to implement.
3. Are you downloading a large number of files at once? If you use the method in the above article to download a large number of files, it may put some pressure on your phone's performance. I will explain this in detail later.

Why does too many downloads cause a certain amount of pressure?

Focus on around line 180 of [HanimeDownloadWorker.kt](app/src/main/java/com/yenaly/han1meviewer/worker/HanimeDownloadWorker.kt):

```kotlin
const val RESPONSE_INTERVAL = 500L

if (System.currentTimeMillis() - delayTime > RESPONSE_INTERVAL) {
    val progress = entity.downloadedLength * 100 / entity.length
    setProgress(workDataOf(PROGRESS to progress.toInt()))
    setForeground(createForegroundInfo(progress.toInt()))
    DatabaseRepo.HanimeDownload.update(entity)
    delayTime = System.currentTimeMillis()
}
```

In my app, I've set an update interval of 500ms, which is equivalent to two database update operations/s/job. In addition, through Flow/LiveData callbacks, when the database detects a data update, it immediately returns a new list with the latest data, which is equivalent to another two callbacks/s/job. If you download a large number of files at once and set the RESPONSE_INTERVAL setting low, this may put a certain amount of strain on the database. In this case, this method is not very effective.

Now that RecyclerView is configured, how do I solve the refresh flickering problem? The method I provided in the original article is not good:

```kotlin
rv.itemAnimator?.changeDuration = 0
```

This code only solves the problem superficially, but the underlying issue persists. Even with a differential refresh using DiffUtil, the update is still global, which is self-deceptive. To see if `holder.binding.pbProgress.setProgress(item.progress, true)` works correctly, try it out. So, how can we achieve this? When the `isDownloading` field changes, the pause button is updated separately; when the `downloadedLength` field changes, the progress bar is updated separately? This is where `payload` comes in.

There are tons of articles on payload, including on StackOverflow and even on Nuggets. A quick search will easily explain it, so I won't go into detail. The key is the getChangePayload method in DiffUtil.ItemCallback and the payloads parameter in onBindViewHolder .

**First** read the article about `payload` usage, then read the following.

I've discovered that while many people have introduced this approach, few have addressed how to efficiently handle multiple fields at once. You might consider using `List<Int>` or `IntArray`, and iterating through each case to handle each situation. In this case, both the time and space complexity are `O(n)`, where `n` is the number of fields you need to monitor. A more clever approach might be to use `Set<Int>` and query each set in `onBindViewHolder` to see if it contains a specific case, reducing the time complexity to `O(1)`. This is fine for infrequent refreshes, but creating a new data structure each time is a burden under high-intensity conditions. So, what should be done?

A simple Bitmap data structure is a good choice. You might have just heard of it, but it's quite common. You'll likely encounter it when opening a new Activity using `Intent#addFlags`. We can use a 32-bit integer value, which only takes four bytes, to implement find (`find`), isEmpty (`isEmpty`), and add (`add`) functions (we only need these functions, and the number of different cases will likely be no more than 32).

聚焦于 [HanimeDownloadingRvAdapter.kt](app/src/main/java/com/yenaly/han1meviewer/ui/adapter/HanimeDownloadingRvAdapter.kt)

> Note: I used BRVAH as a replacement for RecyclerView, so the specific method may not be the same as RecyclerView, but the usage is basically the same.

```kotlin
companion object {
    private const val DOWNLOADING = 1 // 0000 0001
    private const val PAUSE = 1 shl 1 // 0000 0010

    val COMPARATOR = object : DiffUtil.ItemCallback<HanimeDownloadEntity>() {
        override fun areContentsTheSame(
            oldItem: HanimeDownloadEntity,
            newItem: HanimeDownloadEntity,
        ): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(
            oldItem: HanimeDownloadEntity,
            newItem: HanimeDownloadEntity,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun getChangePayload(
            oldItem: HanimeDownloadEntity,
            newItem: HanimeDownloadEntity,
        ): Any {
            // Assume that only progress is different from the original
            where bitset = 0
            // bitset == 0000 0000
            if (oldItem.progress != newItem.progress || oldItem.downloadedLength != newItem.downloadedLength)
                bitset = bitset or DOWNLOADING
            	// bitset == 0000 0001
            if (oldItem.isDownloading != newItem.isDownloading)
                bitset = bitset or PAUSE
            	// Do not pass here
            return bitset
            // return 0000 0001
        }
    }
}
```

```kotlin
override fun onBindViewHolder(
    holder: DataBindingHolder<ItemHanimeDownloadingBinding>,
    position: Int,
    item: HanimeDownloadEntity?,
    payloads: List<Any>,
) {
    // If the payloads list is empty or 0000 0000, no modification is required
    if (payloads.isEmpty() || payloads.first() == 0)
        return super.onBindViewHolder(holder, position, item, payloads)
    item.notNull()
    val bitset = payloads.first() as Int
    // 0000 0001 & 0000 0001 = 0000 0001 != 0000 0000
    // Modify progress related controls
    if (bitset and DOWNLOADING != 0) {
        holder.binding.tvSize.text = spannable {
            item.downloadedLength.formatFileSize().text()
            " | ".span { color(Color.RED) }
            item.length.formatFileSize().span { style(Typeface.BOLD) }
        }
        holder.binding.tvProgress.text = "${item.progress}%"
        holder.binding.pbProgress.setProgress(item.progress, true)
    }
    // 0000 0001 & 0000 0010 = 0000 0000 == 0000 0000
    // Do not pass below
    if (bitset and PAUSE != 0) {
        holder.binding.btnStart.handleStartButton(item.isDownloading)
    }
}
```

In this way, a relatively efficient differential refresh is achieved.

### CI Update Channel

#### You can learn

#### Key Files

#### explain

Your software is highly extensible, but due to limitations in the subject matter or sheer laziness, it's inconvenient to build your own server to read these extension files. You'd also like to allow users to get real-time updates through other channels (for example, if someone uploads an extension file, I merge it into the main branch, and users get the update a few minutes later, without having to build a package myself). However, not everyone needs these extensions (and if they don't want to use your feature, the constant releases will annoy users; if you keep releasing packages yourself, you'll also be annoyed). So, can you provide users with two channels? One is a stable update channel, where you release your own versions; the other is a development version, automatically built by GitHub, which guarantees the latest features (with the latest extensions integrated immediately) but not stability.

The answer is yes. Actually, I had no idea how to do it before, but @NekoOuO sent me the recipe for [Foolbar/EhViewer](https://github.com/FooIbar/EhViewer/), and I copied it without a second thought. But no one has explained how to do it in detail, so I'm going to explain it today.

First, read the basics of GitHub CI.

Google and Nuggets are full of tutorials. Start by looking up how to use it and configuring it. Initially, the requirements aren't too complicated. Once you've uploaded a commit, GitHub CI starts working and successfully builds. That's enough to get you started. Don't worry about what happens after the build or anything else. If everything goes smoothly, move on to the following steps.

To be updated...

### Shared key H frame

#### You can learn

1. How to make full use of Kotlin's collection operation functions to sort, classify, and even flatten individual JSON files?

   Related functions: `groupBy`, `flatMap`, `sortedWith` `=>` `compareBy`, `thenBy`

#### Key Files

- [HKeyframes folder](app/src/main/assets/h_keyframes) - stores all shared key H frames
- [DatabaseRepo.kt](app/src/main/java/com/yenaly/han1meviewer/logic/DatabaseRepo.kt) - Handles shared key H frames
- [SharedHKeyframesRvAdapter.kt](app/src/main/java/com/yenaly/han1meviewer/ui/adapter/SharedHKeyframesRvAdapter.kt) - 界面 Adapter
- [HKeyframeEntity.kt](app/src/main/java/com/yenaly/han1meviewer/logic/entity/HKeyframeEntity.kt) - Related entity class

#### explain

Many people laughed when they saw the [HKeyframes folder](app/src/main/assets/h_keyframes). All the JSON files are put together. The author must be a fool. He doesn’t even know how to classify them into folders?

Do you think I didn't think of this? First of all, why is it not working well to divide folders:

1. Folder-based access doesn't allow for all key H-frames to be retrieved all at once. For example, if you're watching a video with `videoCode` set to `114514`, I can directly retrieve the corresponding file in that folder, without searching through all the folders. This is similar to the difference between a List and a Map.
2. Assuming that after the folders are divided, create a JSON in the root directory to write the code of which folder contains which movie. This is not impossible, but it will increase the burden on others who want to provide shared H-frames.

This is mainly a historical issue, I'm too lazy to fix it. Kotlin has so many collection operation functions, isn't it easy to sort by group?

I'll give you a key H-frame JSON file and ask you to figure out how to convert it into the following format:

Format:

```
- Series 1
	- Series 1 Episode 1
	- Series 1 Episode 2
	- Series 1 Episode 3
- Series 2
	- Series 2 Episode 1
	- Series 2 Episode 2
```

Random key H frame:

> Please note that the `videoCode` on this website is not arranged in sequence, and there may be a video from another series between the first and second episodes.

```json
{
  "videoCode": "114514",
  "group": "Series 2",
  "title": "Series 2 Episode 2",
  "episode": 2,
  "author": "Bekki Chen",
  "keyframes": [
    {
      "position": 482500,
      "prompt": null
    },
    {
      "position": 500500,
      "prompt": null
    },
    {
      "position": 556000,
      "prompt": null
    },
    {
      "position": 777300,
      "prompt": null
    }
  ]
}
```

You might want to use a Map for classification, but RecyclerView can't pass Maps. So how can you flatten it into a List and still implement multiple RecyclerView layouts? If you need to implement multiple RecyclerView layouts with two completely different data types, you'll have to rely on APIs. For example, in this app, the shared key H-frame interface has different data for the title and content.

聚焦于 [HKeyframeEntity.kt](app/src/main/java/com/yenaly/han1meviewer/logic/entity/HKeyframeEntity.kt)

```kotlin
interface MultiItemEntity {
    val itemType: Int
}

interface HKeyframeType : MultiItemEntity {
    companion object {
        const val H_KEYFRAME = 0
        const val HEADER = 1
    }
}
```

Then I won’t say much about HKeyframeEntity and HKeyframeHeader. Just give the correct `itemType` override to the corresponding `itemType` field.

Now the question is how to read those shared key H frames and flatten them?

Focus on [DatabaseRepo.kt](app/src/main/java/com/yenaly/han1meviewer/logic/DatabaseRepo.kt)

```kotlin
@OptIn(ExperimentalSerializationApi::class)
fun loadAllShared(): Flow<List<HKeyframeType>> = flow {
    val res = applicationContext.assets.let { assets ->
        // The assets.list method gets a list of all files in the folder
        assets.list("h_keyframes")?.asSequence() // Convert it to a sequence
            ?.filter { it.endsWith(".json") } // Pick out the ones ending with json
            ?.mapNotNull { fileName -> // Map the file name to a file, and then convert the file into an entity
                try {
                    // assets.open method opens the file
                    assets.open("h_keyframes/$fileName").use { inputStream ->
                        Json.decodeFromStream<HKeyframeEntity>(inputStream)
                    }
                } catch (e: Exception) { // Return null if a problem occurs
                    e.printStackTrace()
                    null
                }
            }
            ?.sortedWith(
                compareBy<HKeyframeEntity> { it.group }.thenBy { it.episode }
            ) // Sort by group first, then by episode
            ?.groupBy { it.group ?: "???" } // Group, create a Map with group as key and a list of all videos under group as value. If group is null, add it to group ???
            ?.flatMap { (group, entities) -> // Provide two parameters, key and value
                listOf(HKeyframeHeader(title = group, attached = entities)) + entities
            } // Key: Flattening, changing the relationship between groups and entities from master-slave to parallel
            .orEmpty() // If list is null, return an empty list of length 0
    }
    emit(res)
}
```

Then set `itemType` in the corresponding RecyclerView, and then configure related functions according to `itemType`.

具体查看 [SharedHKeyframesRvAdapter.kt](app/src/main/java/com/yenaly/han1meviewer/ui/adapter/SharedHKeyframesRvAdapter.kt) 
