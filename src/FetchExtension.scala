package org.nlogo.extension.fetch

import java.lang.{ Boolean => JBoolean }
import java.io.IOException
import java.net.{ MalformedURLException, URL }
import java.nio.file.{ Files, InvalidPathException, NoSuchFileException, Path, Paths }
import java.util.Base64

import org.nlogo.api.{ Argument, Command, Context, DefaultClassManager, ExtensionException, PrimitiveManager, Reporter }
import org.nlogo.core.{ LogoList, Syntax }
import org.nlogo.nvm.HaltException

import scala.language.reflectiveCalls

class FetchExtension extends DefaultClassManager {

  override def load(manager: PrimitiveManager): Unit = {
    manager.addPrimitive("file"           , FilePrim)
    manager.addPrimitive("file-async"     , FileAsyncPrim)
    manager.addPrimitive("url"            , URLPrim)
    manager.addPrimitive("url-async"      , URLAsyncPrim)
    manager.addPrimitive("user-file"      , UserFilePrim)
    manager.addPrimitive("user-file-async", UserFileAsyncPrim)
  }

  private object FilePrim extends Reporter {
    override def getSyntax = Syntax.reporterSyntax(right = List(Syntax.StringType), ret = Syntax.StringType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val path = getPath(args(0).getString)
      slurp(path.toUri.toURL.openConnection().getContentType) { Files.readAllBytes(path) }
    }
  }

  private object FileAsyncPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType, Syntax.CommandType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val path     = getPath(args(0).getString)
      val command  = args(1).getCommand
      val contents = slurp(path.toUri.toURL.openConnection().getContentType) { Files.readAllBytes(path) }
      command.perform(context, Array(contents))
    }
  }

  private object URLPrim extends Reporter {
    override def getSyntax = Syntax.reporterSyntax(right = List(Syntax.StringType), ret = Syntax.StringType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val url = newURL(args(0).getString)
      slurp(url.openConnection().getContentType) {
        using(url.openStream()) {
          (urlConn) => Stream.continually(urlConn.read).takeWhile(_ != -1).map(_.toByte).toArray
        }
      }
    }
  }

  private object URLAsyncPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType, Syntax.CommandType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val url      = newURL(args(0).getString)
      val command  = args(1).getCommand
      val contents =
        slurp(url.openConnection().getContentType) {
          using(url.openStream()) {
            (urlConn) => Stream.continually(urlConn.read).takeWhile(_ != -1).map(_.toByte).toArray
          }
        }
      command.perform(context, Array(contents))
    }
  }

  private object UserFilePrim extends Reporter {
    override def getSyntax = Syntax.reporterSyntax(right = List(), ret = Syntax.StringType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val falseOrPath = UserFile.userFileSync(context.workspace)
      falseOrPath.fold(identity, pathStr => {
        val path = getPath(pathStr)
        slurp(path.toUri.toURL.openConnection().getContentType) { Files.readAllBytes(path) }
      })
    }
  }

  private object UserFileAsyncPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.CommandType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val command = args(0).getCommand
      UserFile.userFileAsync(context.workspace) {
        _ match {
          case JBoolean.FALSE =>
            command.perform(context, Array(JBoolean.FALSE))
          case pathStr: String =>
            val path     = getPath(pathStr)
            val contents = slurp(path.toUri.toURL.openConnection().getContentType) { Files.readAllBytes(path) }
            command.perform(context, Array(contents))
        }
      }
    }
  }

  private def using[T <: { def close(): Unit }, U](t: T)(f: (T) => U): U =
    try { f(t) } finally { t.close() }

  private def getPath(path: String): Path = {
    try Paths.get(path)
    catch {
      case ex: InvalidPathException =>
        throw new ExtensionException(s"Could not parse path: ${ex.getMessage}", ex)
    }
  }

  private def newURL(url: String): URL = {
    try new URL(url)
    catch {
      case ex: MalformedURLException =>
        throw new ExtensionException(s"Ensure that your URL is prefixed with a valid protocol (e.g. 'http://', 'https://', 'file://'): ${ex.getMessage}", ex)
    }
  }

  private def slurp(mimeType: => String)(f: => Array[Byte]): String = {

    val contentType =
      try mimeType
      catch {
        case ex: IOException =>
          throw new ExtensionException(s"Failed to probe content type (probably because no resource exists at the given location): ${ex.getMessage}", ex)
      }

    val bytes =
      try f
      catch {
        case ex: NoSuchFileException =>
          throw new ExtensionException(s"File not found: ${ex.getMessage}", ex)
        case ex: IOException =>
          throw new ExtensionException(s"Unable to fetch (probably because no resource exists at the given location): ${ex.getMessage}", ex)
      }

    val CharsetRegex = "(?i)^text/.*?;\\s*charset=(.*)$".r

    contentType match {
      case CharsetRegex(charset) =>
        new String(bytes, charset)
      case x =>
        val str = new String(bytes, "UTF-8")
        if (bytes.sameElements(str.getBytes()))
          str
        else
          s"data:$contentType;base64,${Base64.getEncoder().encodeToString(bytes)}"
    }

  }

}
