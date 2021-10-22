# Android VideoView cache
Cache wrapper for standart android VideoView

It uses okhttp and rxjava inside. Nothing special is needed to use.

# Setup:  

[![](https://jitpack.io/v/Andrew0000/Android-VideoView-cache.svg)](https://jitpack.io/#Andrew0000/Android-VideoView-cache)

1. Add `maven { url 'https://jitpack.io' }` to the `allprojects` or `dependencyResolutionManagement` section in top-leve `build.gradle` or `settings.gradle`.  
For example (`settings.gradle`):
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter() // Warning: this repository is going to shut down soon
        maven { url "https://jitpack.io" }
    }
}
```
2. Add implementation `'com.github.Andrew0000:Android-VideoView-cache:1.0.3'` (chek latest version) to the module-level `build.gradle`

# Usage:
Like a VideoView. 

For example in xml:

    <crocodile8008.videoviewcache.lib.VideoViewCached
        android:id="@+id/cachedVideoView2"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintHeight_max="200dp"/>

And then start loading:
```
cachedVideoView2.playUrl("https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_480_1_5MG.mp4")
```
That's all!  
Video will be downloaded, cached and played.  
On the next invocation it will be loaded from cache.  
