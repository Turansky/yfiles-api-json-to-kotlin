[![CI Status](https://github.com/turansky/yfiles-kotlin/workflows/declarations/badge.svg)](https://github.com/turansky/yfiles-kotlin/actions)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/turansky/yfiles/com.github.turansky.yfiles.gradle.plugin/maven-metadata.xml.svg?label=plugin&logo=gradle)](https://plugins.gradle.org/plugin/com.github.turansky.yfiles)
[![IntelliJ IDEA Plugin](https://img.shields.io/jetbrains/plugin/v/13384-yfiles?label=plugin&logo=intellij-idea)](https://plugins.jetbrains.com/plugin/13384-yfiles/)
[![IntelliJ IDEA Plugin](https://img.shields.io/jetbrains/plugin/d/13384-yfiles?logo=intellij-idea)](https://plugins.jetbrains.com/plugin/13384-yfiles/)
[![Kotlin](https://img.shields.io/badge/kotlin-1.3.72-blue.svg?logo=kotlin)](http://kotlinlang.org)

# Kotlin/JS declarations generator for yFiles

## [Gradle Plugin](gradle-plugin)
Resolve inheritance problems
> Includes temp WA for [`KT-34770`](https://youtrack.jetbrains.com/issue/KT-34770).
> Configurable properties required for yFiles

## [IDEA Plugin](idea-plugin)
Check [inheritance rules](gradle-plugin) on the fly

## Table of contents
* [Generation](#generation)
* [YClass](#yclass)
  * [Metadata](#metadata)
  * [Primitive types](#primitive-types)
  * [Cast extensions](#cast-extensions)
  * [Lookup extensions](#lookup-extensions)
  * [Type parameter](#type-parameter) 
* [Factory methods](#factory-methods)
* [Flags](#flags)
* [`for` loop](#for-loop)
  * [`IEnumerable`](#ienumerable)
  * [`ICursor`](#icursor)
* [Observable <sup>**new**</sup>](#observable)  
* [TimeSpan extensions](#timespan)
* [Resources Defaults](#resources-defaults)
* [KDoc](#kdoc)
  * [Online Documentation](#online-documentation) 

## Generation
* Run `./gradlew build`
* Check source folders

| Declarations                | Source folder                                              |
| :---                        | :---                                                       |
| [yFiles for HTML][11]       | [`yfiles-kotlin`](libraries/yfiles-kotlin/src/main/kotlin) |
| [VSDX Export][21]           | [`vsdx-kotlin`](libraries/vsdx-kotlin/src/main/kotlin)     |
  
## Description
| JS library                  | [yFiles for HTML][11] | [VSDX Export][21] |
| :---                        |         :---:         |      :---:        |
| Documentation               |        [API][12]      |     [API][22]     |
| Module                      |        `yfiles`       |   `yfiles/vsdx`   |
| Version                     |         `23.0.0`      |      `1.1.1`      |
| Module format               |         `ES6`         |       `ES6`       |
| **Kotlin/JS Declarations**  |  **`yfiles-kotlin`**  | **`vsdx-kotlin`** |
| Nullability fixes           |         3200+         |         -         |
| Numberability*              |           ✔           |         ✔         |
| Strict [`Class`][31] generic  |           ✔           |         ✔         |
| Trait support**             |           ✔           |         ✔         |
| Operators                   |           ✔           |         ✔         |
| Operator aliases            |           ✔           |         ✔         |

\* - `Int`, `Double` instead of `Number`<br>
\** - via extension methods

#### Related issues
* [`KT-34770`](https://youtrack.jetbrains.com/issue/KT-34770) - Non-configurable properties
* [`No Xcode or CLT version detected!`](https://github.com/nodejs/node-gyp/issues/1927#issuecomment-544507444) - For `macOS Catalina`

## `YClass`

#### Metadata
```Kotlin
// JS: IVisibilityTestable.$class
val clazz = IVisibilityTestable.yclass

// JS: IVisibilityTestable.isInstance(o)
val isInstance = IVisibilityTestable.isInstance(o)
```

#### Primitive types
```Kotlin
Boolean.yclass   // YBoolean.$class
Double.yclass    // YNumber.$class
Int.yclass       // YNumber.$class
String.yclass    // YString.$class
```

#### Cast extensions
```Kotlin
fun (o:Any?) {
    val isNode:Boolean = o yIs INode

    val optNode:INode? = o yOpt INode

    val node:INode = o yAs INode
}
```

#### Lookup extensions
```Kotlin
val graph: IGraph = DefaultGraph()
val node = graph.createNode()

// for classes
val t11 = node lookup TimeSpan.yclass // 'TimeSpan?'
val t12 = node lookup TimeSpan        // 'TimeSpan?'

val t13: TimeSpan? = node.lookup()    // reified lookup type
val t14 = node.lookup<TimeSpan>()     // 'TimeSpan?'

val t21 = node lookupValue TimeSpan.yclass // 'TimeSpan'
val t22 = node lookupValue TimeSpan        // 'TimeSpan'

val t23: TimeSpan = node.lookupValue()    // reified lookup type
val t24 = node.lookupValue<TimeSpan>()    // 'TimeSpan'

// for interfaces
val h11 = node lookup IHitTestable.yclass      // 'IHitTestable?'
val h12 = node lookup IHitTestable             // 'IHitTestable?'

val h21 = node lookupValue IHitTestable.yclass // 'IHitTestable'
val h22 = node lookupValue IHitTestable        // 'IHitTestable'
```

#### Type parameter
```Kotlin
val clazz:YClass<IVisibilityTestable> = IVisibilityTestable.yclass

// strict lookup
val visibilityTestable:IVisibilityTestable? = renderer.lookup(IVisibilityTestable.yclass)
val boundsProvider:IBoundsProvider? = renderer.lookup(IBoundsProvider.yclass)
```

## Factory methods

Via `apply`
```Kotlin
val layout = HierarchicLayout().apply {
    layoutOrientation = LEFT_TO_RIGHT
    automaticEdgeGrouping = true
    gridSpacing = 20.0
}
```

Via factory method
```Kotlin
val layout = HierarchicLayout {
    layoutOrientation = LEFT_TO_RIGHT
    automaticEdgeGrouping = true
    gridSpacing = 20.0
}
```

#### Related issues
* [`KT-31126`](https://youtrack.jetbrains.com/issue/KT-31126) - Invalid JS constructor call (primary ordinary -> secondary external)

## Flags
Some yFiles enums are marked as `flags`.
* Use `or` infix method to combine `flags`
* Use `in` operator to check if `flags` are applied
```Kotlin
import yfiles.graph.GraphItemTypes.*
import yfiles.input.GraphViewerInputMode
import yfiles.lang.contains
import yfiles.lang.or

val inputMode = GraphViewerInputMode {
    clickableItems = NODE or EDGE or LABEL
}

val nodesAreClickable = NODE in inputMode.clickableItems // true
```

## `for` loop

#### `IEnumerable`
```Kotlin
import yfiles.collections.asSequence
import yfiles.collections.iterator

val graph: IGraph = DefaultGraph()
// ...

// iterator() extension allows for loops for IEnumerable
for (node in graph.nodes) {
    println("Node layout: ${node.layout}")
}

// asSequence() extension
graph.nodes
    .asSequence()
    .forEach { println("Node layout: ${node.layout}")}
```

#### `ICursor`
```Kotlin
import yfiles.algorithms.asSequence
import yfiles.algorithms.iterator

val graph: Graph = DefaultLayoutGraph()
// ...

// iterator() extension allows for loops for ICursor
for (node in graph.getNodeCursor()) {
    println("Node index: ${node.index}")
}

// asSequence() extension
graph.getNodeCursor()
    .asSequence()
    .forEach { println("Node index: ${node.index}") }
```

## Observable
```Kotlin
import yfiles.lang.observable

class User {
    var name by observable("Frodo")
}
```

will have the same effect as

```JavaScript
class User {
  #name = 'Frodo'

  constructor() {
    makeObservable(this)
  }
    
  get name() {
    return this.#name 
  }
  
  set name(value) {
    if (this.#name !== value) {
      this.#name = value
      this.firePropertyChanged('name') 
    } 
  }
}
```

##### Details
- [`makeObservable`](https://docs.yworks.com/yfileshtml/#/search/makeObservable)
- [`firePropertyChanged`](https://docs.yworks.com/yfileshtml/#/dguide/custom-styles_template-styles%23_notifying_of_property_changes)

## `TimeSpan`
```Kotlin
val c: TimeSpan = 2.hours
val o: TimeSpan = 0.minutes
val d: TimeSpan = 1.seconds
val e: TimeSpan = 3.milliseconds
```

## Resources Defaults
[What is resources defaults?][41]
```Kotlin
import yfiles.lang.ResourceKeys.COPY
import yfiles.lang.ResourceKeys.COPY_KEY
import yfiles.lang.Resources.invariant
import yfiles.lang.get

fun main() {
    println(invariant[COPY]) // Copy
    println(invariant[COPY_KEY]) // Action+C;Ctrl+Ins
}
```

## KDoc
Generated!

#### Online Documentation
![Example](assets/online-documentation.gif)

#### Related issues
* [`KT-32815`](https://youtrack.jetbrains.com/issue/KT-32815) - Broken links with double anchor `#`
* [`IDEA-219818`](https://youtrack.jetbrains.com/issue/IDEA-219818) - Broken links with double anchor `#`
* [`KT-32640`](https://youtrack.jetbrains.com/issue/KT-32640) - Broken markdown links in `@see` block
* [`KT-32720`](https://youtrack.jetbrains.com/issue/KT-32720) - `@see` is recommended?

[11]: https://www.yworks.com/products/yfiles-for-html
[12]: http://docs.yworks.com/yfileshtml/

[21]: https://www.yworks.com/products/yfiles-for-html/vsdx-export
[22]: https://docs.yworks.com/vsdx-html/

[31]: https://docs.yworks.com/yfileshtml/#/api/Class

[41]: http://docs.yworks.com/yfileshtml/#/dguide/customizing_concepts_resource-keys#resource_defaults
