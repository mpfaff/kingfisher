---
---

# API

Kingfisher provides a robust (WIP) API to scripts.

The full Javadoc is available [here]({{ '/api/javadoc' | relative_url }}), but most of the APIs in there are not intended to be used by scripts and are not exported to them.

## Core

Scripts of all languages and during all phases have access to a set of core APIs that behave the same irrespective of the phase. See [the Javadoc]({{ '/api/javadoc/kingfisher/scripting/Api.html' | relative_url }}) for a listing of these.

## Registration

Scripts must register themselves to handle various events. See [the Javadoc]({{ '/api/javadoc/kingfisher/scripting/RegistrationApi.html' | relative_url }}) for a listing of these.

## Request handling

See [the Javadoc]({{ '/api/javadoc/kingfisher/requests/RequestScriptThread.Api.html' | relative_url }}).

## Language-specific APIs

The supported languages each have their own set of available APIs.

### [JavaScript]({{ '/api/javadoc/kingfisher/interop/js/JSApi.html' | relative_url }})

JavaScript has a limited selection of the standard library available.

The language specific APIs implemented closely resemble missing standard library and NodeJS APIs.

### Python

Python has most if not all of the standard library available to use.

No additional language-specific APIs are available.
