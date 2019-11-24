[![CI Status](https://github.com/turansky/yfiles-kotlin/workflows/CI/badge.svg)](https://github.com/turansky/yfiles-kotlin/actions)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/turansky/yfiles/com.github.turansky.yfiles.gradle.plugin/maven-metadata.xml.svg?label=plugin&logo=gradle)](https://plugins.gradle.org/plugin/com.github.turansky.yfiles)
[![Kotlin](https://img.shields.io/badge/kotlin-1.3.60-blue.svg)](http://kotlinlang.org)
![License](https://img.shields.io/github/license/turansky/yfiles-kotlin)

# yFiles Gradle Plugin for Kotlin/JS

## Goal
- Safe [interface implementing](http://docs.yworks.com/yfileshtml/#/dguide/framework_basic_interfaces#framework_implementing_interfaces)
- OOB `YObject` support

## Interface(s) implementing

#### Example
```Kotlin
// Generated prototype (JS): 
// MyVisualTemplate.prototype = Object.create(BaseClass(IVisualTemplate).prototype) 
class MyVisualTemplate: IVisualTemplate {
    /* body */
}

// Generated prototype (JS): 
// MyArrow.prototype = Object.create(BaseClass(IArrow, IVisualCreator, IBoundsProvider).prototype)
class MyArrow: IArrow, IVisualCreator, IBoundsProvider {
    /* body */
}
```

#### Requirements
- Only yFiles interfaces implemented
- No extended class

## Custom `YObject`

#### Example
```Kotlin
// Generated JS: 
// Class.fixType(SelectionProvider) 
class SelectionProvider : YObject {
    fun isSelected(item: IModelItem): Boolean = /* ... */
}

// Generated JS: 
// Class.fixType(HighlightProvider)
class HighlightProvider : YObject {
    fun isHighlighted(item: IModelItem): Boolean = /* ... */
}
```

#### Requirements
- No extended interfaces