# Fetch Extension

## What is it?

This extension is for reading files and URLs within NetLogo, with an eye towards providing a compatible API for usage in NetLogo Web.

## Why use it?

  1. Because NetLogo doesn't come with primitives for reading from URLs
  2. While the Web extension provides the ability to read from URLs for more complicated use cases, it won't be implemented in NetLogo Web anytime soon
  3. Because it provides async primitives that will allow for a common API between desktop NetLogo and NetLogo Web

If you just want to read files, and you are happy with NetLogo's `file-open` (et al), you do *not* need this extension.  If, however, you want to read URLs, or want interoperability with NetLogo Web, then this is the extension for you.

Note: The synchronous primitives here might not work in the NetLogo Web version (due to limitations of JavaScript), so stick to the async ones if you can.

## Primitives

| Prim Name         | Arguments             | Behavior
| ----------------- | --------------------- | --------
| `file`            | *filepath*            | Synchronously read the file at `filepath` and return its contents as a string
| `file-async`      | *filepath* *callback* | Asynchronously read the file at `filepath`, whenever that is done, run `callback`, passing the contents of the file as `callback`'s sole argument
| `url`             | *url*                 | Synchronously read the URL at `url` and return its contents as a string
| `url-async`       | *url* *callback*      | Asynchronously read the URL at `url`, whenever that is done, run `callback`, passing the content from the URL as `callback`'s sole argument
| `user-file`       |                       | Synchronously open a file dialog and, whenever the user selects a file, return the contents of that file as a string (base64-encoded if the file is not a text document), or return `false` if the user cancels the file dialog
| `user-file-async` | *callback*            | Asynchronously open a file dialog and, whenever the user selects a file, pass the contents of that file as a string (base64-encoded if the file is not a text document) as `callback`'s sole argument, or pass in `false` if the user cancels the file dialog

## Example Code

This extension was primarily intended as a companion to the `import-a` extension, so our example code will use that.

This first set contains only NetLogo Web compatible examples.

```netlogo
extensions [import-a fetch]

; Basic printing of a string (no extensions involved)
to test-fetch-reporter
  clear-all
  show "I'm a little reporter, short and stout.  Here is my input.  Here is my out."
end

; Printing of the contents of a file, using the asynchronous primitive in this extension
to test-fetch-user-file-async
  clear-all
  fetch:user-file-async show
end

; Printing of the contents of a file, async, without using the 'concise' anonproc syntax
to test-fetch-user-file-verbose-syntax
  clear-all
  fetch:user-file-async [text -> show text]
end

; Importing world state from a file and then running some other code once it has completed
to test-world-user-file-and-then
  clear-all
  fetch:user-file-async [
    text ->
      import-a:world text
      show "Success!"
  ]
end

; Printing of the content from a URL
to test-fetch-url-async
  clear-all
  fetch:url-async "http://www.netlogoweb.org/" show
end

; Printing content from a URL and then running some other code once it has completed
to test-fetch-url-and-then
  clear-all
  fetch:url-async "http://www.netlogoweb.org/" [
    text ->
      show text
      show "Success!"
  ]
  show "Procedure complete."
end
```

The rest of these examples are not NetLogo Web compatible.

```netlogo
extensions [import-a fetch]

; Printing of the contents of a file, using the synchronous primitive in this extension
to test-fetch-file-sync
  clear-all
  show (fetch:file user-file)
end

; Printing of the contents of a file, using the asynchronous primitive in this extension
to test-fetch-file-async
  clear-all
  fetch:file-async user-file show
end

; Printing of the contents of a file, async, without using the 'concise' anonproc syntax
to test-fetch-file-verbose-syntax
  clear-all
  fetch:file-async user-file [text -> show text]
end

; Printing of the contents of a file, using the synchronous primitive in this extension
to test-fetch-user-file-sync
  clear-all
  show fetch:user-file
end

; Printing of the content from a URL, using the synchronous primitive in this extension
to test-fetch-url-sync
  clear-all
  show (fetch:url (word "file://" user-file))
end

; Printing of the content from a URL
to test-fetch-url-async
  clear-all
  fetch:url-async (word "file://" user-file) show
end

; Importing world state from a file
to test-world-file
  clear-all
  fetch:file-async user-file import-a:world
end

; Importing world state from a file and then running some other code once it has completed
to test-world-file-and-then
  clear-all
  fetch:file-async user-file [
    text ->
      import-a:world text
      show "Success!"
  ]
end

; Importing world state from a URL
to test-world-url
  clear-all
  fetch:url-async (word "file://" user-file) import-a:world
end

; Importing world state from a URL and then running some other code once it has completed
to test-world-url-and-then
  clear-all
  fetch:url-async user-file [
    text ->
      import-a:world text
      show "Success!"
  ]
end
```

## What are asynchronous primitives?  How are they different from synchronous ones?

This is a problem that really only comes up because we want a compatible API with NetLogo Web (which runs JavaScript code).  Let's talk about this, using a small, abstract example:

```
A(F)
X(Y)
C(G)
```

Where `A`, `X`, and `C` are functions that accept functions as arguments, as `F`, `Y`, and `G` are plain, not-yet-executed functions.

In most programming languages, statements are synchronous by default, but, using this example that has been provided, let's say that the statement `X(Y)` might be either synchronous or asynchronous.  In the case that `X` is synchronous, the code runs exactly how you would intuitively expect it to: `A` executes `F`, then `X` executes function `Y`, then `C` executes `G`.  In synchronous environments, the order in which the code is run is linear and predictable.

JavaScript (the language that runs in your web browser) is a language that only dedicates one processor thread to each web page (not strictly true, but true enough for our purposes here).  What that means is that, if some long-running process is hogging that one thread, then you might have difficulties scrolling around the page, clicking on elements, typing, or seeing visual updates to the page, since none of the code for those things can be run until the long-running function has finished.

So, going back to our example, it would be problematic in the browser if `Y` were a long-running function (like something that reads a large file off of your disk).  If we tried to run `Y` *immediately* when we reached that line, it would greatly delay running `C(G)`, and would also slow down any browser interactions (as described in the previous paragraph).

JavaScript handles this situation by making long-running operations *asynchronous*.  What that effectively means is that, when running the example code above, `A` will execute `F`, and then `X` will tell the browser to run `Y` when it gets a chance, and, without waiting for `Y` to be executed, will then move on to having `C` execute `G`.  When does `Y` get executed?  We don't know, and we can't predict it.  It could be before `C` runs `G`; it could be right after; it could be five minutes from now.  The code does not run linearly.  And, if we had some code that relied on the result of `Y`, we would essentially have to extend `Y` to append that code and run after the main code of `Y` (see the `test-world-file-and-then` example below for a demonstration of this).

So when we have an asynchronous primitive, like in `fetch:file-async user-file [contents -> show contents]`, that means that `[contents -> show contents]` could first run at any time after that line of code has been executed, and we can't predict precisely when.

The benefits of this are debatable, but it seems likely that, for most people, the only benefits of using the `async` primitives will be that they work in NetLogo Web (not that that is a minor thing).

## I tried reading a file, and I got a bunch of gobbledegook back.  Why?

If you try to read from a source (i.e. file or URL) that can be decoded as UTF-8 (plain text), then the extension will read the source as UTF-8 (plain text).  If, however, it cannot be decoded as plain text, `fetch` will convert the source to [base64-encoded plain text](https://en.wikipedia.org/wiki/Base64).

Note that the other reasonable alternative would be for the extension to convert your source to a list of numbers (bytes).  But it seems more likely that someone would want to deal with the base64 representation of the source (especially when using it with the `import-a` extension), so we went with base64.

## I tried fetching a URL, and I didn't get a result back.  There's definitely something at that URL.  Why did the operation fail?

When using `url` and `url-async` in NetLogo Web, you need to be mindful of [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS).  CORS is a system used by browsers to prevent, for example, Website A reading your browser's cookies and making a request to Website B to perform a privileged action on Website B (such as reading your e-mails through a web interface).  That sort of scenario poses a clear security issue.  There are only two times when this sort of request is ever allowed: (a) when Website A and Website B are running on the same web domain (URL), and (b) when Website B explicitly opts into this by setting their HTTP responses' `Access-Control-Allow-Origin` headers.  Many websites do not set this header, and, as such, do not allow cross-origin requests.

If you run into this problem, try hosting your resource (e.g. the image that you want to fetch) on the same website that the NetLogo Web is running on.  (If you're using the NetLogo desktop application, this issue never comes up, because the NetLogo application isn't running on any web domain.)  If you cannot do that, try using a CORS proxy service (like [this one](https://cors-anywhere.herokuapp.com)) or re-host the resource, yourself, on a different service—one that has CORS enabled.

## I tried fetching a resource and printing it out to the Command Center, and now NetLogo is super slow!  What happened?!

When you fetch things, you receive their contents as strings.  When you fetch a very large thing, you will be given a very large string.  The NetLogo Command Center does not take well to printing out very large strings, and will likely slow to a crawl if given one.  However, do note that you can speed NetLogo back up again by clicking the "Clear" button next to the Command Center output window, thereby disposing of the enormously long string (and all other strings in the Command Center).

## Building

Open it in SBT.  If you successfully run `package`, `fetch.jar` is created.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo Fetch extension is in the public domain.  To the extent possible under law, Uri Wilensky has waived all copyright and related or neighboring rights.
