SBT JS Plugin
=============

[Simple Build Tool] plugin for compiling Javascript files from multiple sources using Google's [Closure compiler].

Copyright (c) 2011 [Dave Gurnell] of [Untyped].

[Simple Build Tool]: http://simple-build-tool.googlecode.com
[Closure compiler]: http://code.google.com/p/closure-compiler
[Dave Gurnell]: http://boxandarrow.com
[Untyped]: http://untyped.com

Installation
============

For SBT 0.11:

Create a `project/plugins.sbt` file and paste the following content into it:

    resolvers += "Untyped Public Repo" at "http://repo.untyped.com"

    addSbtPlugin("untyped" % "sbt-js" % "0.6-SNAPSHOT")

Then, in your build.sbt file, put:

    seq(jsSettings:_*)

If you're using [xsbt-web-plugin](https://github.com/siasia/xsbt-web-plugin "xsbt-web-plugin"), add the output files to the webapp with:

    // add managed resources to the webapp
    (webappResources in Compile) <+= (resourceManaged in Compile)

To change the directory that is scanned, use:

    (sourceDirectory in (Compile, JsKeys.js)) <<= (sourceDirectory in Compile)(_ / "path" / "to" / "js-files")

To make the js task run with compile:

    // make compile depend on js
    (compile in Compile) <<= compile in Compile dependsOn (JsKeys.js in Compile)

The plugin is currently untested under SBT 0.10. If you manage to get it to work,
let me know and I'll update these docs.

Usage
=====

To compile Javascript sources, use the `js` command in sbt. Read the installation
instructions above to see how to include Javascript compilation as part of the regular
`compile` command.

The default behaviour of the plugin is to scan your `src/main` directory
and look for two types of files:

 - Javascript files, with extension `.js`
 - Javascript manifest files, with extension `.jsm` or `.jsmanifest`

These files are compiled and minified using Google's Closure compiler and
placed in equivalent locations under `target/scala-2.9.x/resource_managed`.

Read on for a description of the handling for each type of file.

### *Require* statements in Javascript files

You can add *require* statements to your Javascript files to specify dependencies
on other files or URLs. Require statements can be of the following forms:

    // require "path/to/my/file.js"

    // require "http://mywebsite.com/path/to/my/file.js"

Required files are *prepended* to the file they appear in, and the whole lot is
passed through the Google Closure compiler for minification. Note the following:

 - paths are resolved relative to the file they appear in;
 
 - the position of a require statement in the source does not matter - dependencies
   are always inserted just before the beginning of the file;

 - if multiple require statements are present in a file, the dependencies are inlined
   in the order they are required;

 - dependencies can be recursive - files can require files that require files;
 
 - woe betide you if you create a recursive dependencies :)

### Javascript manifest files

Javascript manifest files are a useful shorthand for building Javascript from a list
of sources. A manifest contains an ordered list of JavaScript source locations. 
For example:

    # You can specify remote files using URLs...
    http://code.jquery.com/jquery-1.5.1.js

    # ...and local files using regular paths
    #    (relative to the location of the manifest):
    lib/foo.js
    bar.js

    # Blank lines and bash-style comments are also supported.
    # These may be swapped for JS-style comments in the future.

The sources are cached and inlined into one large Javascript file, which is then
passed to the Closure compiler. The compiler outputs a file of the same name and 
relative path of the manifest, but with a `.js` extension. For example, if your manifest
file is at `src/main/javascript/static/js/kitchen-sink.jsm` in the source tree, the final
path would be `resource_managed/main/static/js/kitchen-sink.js` in the target tree.

Templating - NOT YET IMPLEMENTED FOR SBT 0.11
================

It is sometime useful to template Javascript files. For example, you might want
scripts to refer to localhost while developing and your live server when
deployed. This plugin supports templating Javascript files using the [Mustache]
format and [Lift style properties] (though the implementation has no dependency
on Lift).

In summary, properties are looked for in `src/main/resources/prop` (by default;
see below for customization). They are in the standard `key=value` Java format. If you
aren't interested in changing your properties depending on your build
configuration just place the properties in `default.props`. Otherwise property
files should be named `modeName.props`, where modeName is the setting of the
`run.mode` system property, which can take on values of `test`, `staging`,
`production`, `pilot`, or `default`. If `run.mode` is not set, `default` is
assumed.

Any Javascript file that contains `.template` will be passed through a Mustache
template processor before being processed by the Google compiler.

Parameters controlling templating are:

   - `closureJsIsTemplated` is function that indicates if a given Javascript
     file should be run through the template processor

   - `closurePropertiesPath` determines where properties are found

[Mustache]: http://mustache.github.com/
[Lift style]: http://www.assembla.com/spaces/liftweb/wiki/Properties

Acknowledgements
================

v0.6+ for SBT 0.11 based on [less-sbt](https://github.com/softprops/less-sbt), Copyright (c) 2011 Doug Tangren.
v0.1-v0.5 for SBT 0.7 based on [Coffee Script SBT plugin], Copyright (c) 2010 Luke Amdor.

Heavily influenced by the [YUI Compressor SBT plugin] by Jon Hoffman.

[Coffee Script SBT plugin]: https://github.com/rubbish/coffee-script-sbt-plugin
[YUI Compressor SBT plugin]: https://github.com/hoffrocket/sbt-yui

Thanks to [Tim Nelson](https://github.com/eltimn) for his work on the SBT 0.11 migration and dramatic improvements to this README.

Licence
=======

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.