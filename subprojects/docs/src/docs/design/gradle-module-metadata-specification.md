# Gradle module metadata specification

_Note: this format is not yet stable and may change at any time. Gradle does not guarantee to offer any long term support for this version of the format._

This document describes version 0.2 of the Gradle module metadata file. A module metadata file describes the contents of a _module_, which is the unit of publication for a particular repository format, such as a module in a Maven repository. This is often called a "package" in many repository formats.

The module metadata file is a JSON file published alongside the existing repository specific metadata files, such as a Maven POM or Ivy descriptor. It adds additional metadata that can be used by Gradle versions and other tooling that understand the format. This allows the rich Gradle model to be mapped to and "tunnelled" through existing repository formats, while continuing to support existing Gradle versions and tooling that does not understand the format. 

The module metadata file is intended to be machine generated rather than written by a human, but is intended to be human readable.

The module metadata file is also intended to fully describe the binaries in the module where it is present so that it can replace the existing metadata files. This would allow a Gradle repository format to be added, for example.

In version 0.2, the module metadata file can describe only those modules that contain a single _component_, which is some piece of software such as a library or application. Support for more sophisticated mappings will be added by later versions.

## Usage in a Maven repository

When present in a Maven module, the file must have extension `module`. For example, in version 1.2 of 'mylib', the file should be called `mylib-1.2.module`.

Gradle ignores the contents of the Maven POM when the module metadata file is present.

## Contents

The file must be encoded using UTF-8.

The file must contain a JSON object with the following values:

- `formatVersion`: must be present and the first value of the JSON object. Its value must be `"0.2"`
- `component`: optional. Describes the identity of the component contained in the module.
- `builtBy`: optional. Describes the producer of this metadata file and the contents of the module.
- `variants`: optional. Describes the variants of the component packaged in the module, if any.

### `component` value

This value must contain an object with the following values:

- `group`: The group of this component. A string
- `module`: The module name of this component. A string
- `version`: The version of this component. A string
- `url`: optional. When present, indicates where the metadata for the component may be found. When missing, indicates that this metadata file defines the metadata for the whole component. 

### `builtBy` value

This value must contain an object with the following values:

- `gradle`: optional. Describes the Gradle instance that produced the contents of the module. 

### `gradle` value

This value must contain an object with the following values:

- `version`: The version of Gradle. A string
- `buildId`: The buildId for the Gradle instance. A string

### `variants` value

This value must contain an array with zero or more elements. Each element must be an object with the following values:

- `name`: The name of the variant. A string. The name must be unique across all variants of the component.
- `attributes`: optional. When missing the variant is assumed to have no attributes.
- `available-at`: optional. Information about where the metadata and files of this variant are available.
- `dependencies`: optional. When missing the variant is assumed to have no dependencies. Must not be present when `available-at` is present.
- `files`: optional. When missing the variant is assumed to have no files. Must not be present when `available-at` is present.

### `attributes` value

This value must contain an object with a value for each attribute. The attribute value must be a string or boolean.

#### Standard attributes

- `org.gradle.usage` indicates the purpose of the variant. See the `org.gradle.api.attributes.Usage` class for more details. Value must be a string.
- `org.gradle.native.debuggable` indicates native binaries that are debuggable. Value must be a boolean.

### `available-at` value

This value must contain an object with the following values:

- `url`: The location of the metadata file that describes the variant. A string. In version 0.2, this must be a path relative to the module.
- `group`: The group of the module. A string
- `module`: The name of the module. A string
- `version`: The version of the module. A string

### `dependencies` value

This value must contain an array with zero or more elements. Each element must be an object with the following values:

- `group`: The group of the dependency.
- `module`: The module of the dependency.
- `version`: The version selector of the dependency. Has the same meaning as in the Gradle DSL.

### `files` value

This value must contain an array with zero or more elements. Each element must be an object with the following values:

- `name`: The name of the file. A string. This will be used to calculate the name of the file in the cache.
- `url`: The location of the file. A string. In version 0.2, this must be a path relative to the module.
- `size`: The size of the file in bytes. A number.
- `sha1`: The SHA1 hash of the file content. A hex string.
- `md5`: The MD5 hash of the file content. A hex string.

## Example

```
{
    "formatVersion": "0.2",
    "component": {
        "group": "my.group",
        "module": "mylib",
        "version": "1.2"
    }
    "builtBy": {
        "gradle": {
            "version": "4.3",
            "buildId": "abc123"
        }
    },
    "variants": [
        {
            "name": "api",
            "attributes": {
                "usage": "java-compile"
            },
            "files": [
                { 
                    "name": "mylib-api.jar", 
                    "url": "mylib-api-1.2.jar",
                    "size": "1453",
                    "sha1": "abc12345",
                    "md5": "abc12345"
                }
            ],
            "dependencies": [
                { "group": "some.group", "module": "other-lib", "version": "3.4" }
            ]
        },
        {
            "name": "runtime",
            "attributes": {
                "usage": "java-runtime"
            },
            "files": [
                { 
                    "name": "mylib.jar", 
                    "url": "mylib-1.2.jar",
                    "size": "4561",
                    "sha1": "abc12345",
                    "md5": "abc12345"
                }
            ],
            "dependencies": [
                { "group": "some.group", "module": "other-lib", "version": "3.4" }
            ]
        }
    ]
}
```
