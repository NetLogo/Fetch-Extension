package org.nlogo.extension.fetch

import java.io.IOException
import java.net.URL
import java.nio.file.{ Files, NoSuchFileException, Paths }
import java.util.Base64

import org.nlogo.api.{ Argument, Command, Context, DefaultClassManager, ExtensionException, PrimitiveManager, Reporter }
import org.nlogo.core.{ LogoList, Syntax }

import scala.language.reflectiveCalls

class FetchExtension extends DefaultClassManager {

  override def load(manager: PrimitiveManager): Unit = {
    manager.addPrimitive("file"      , FilePrim)
    manager.addPrimitive("file-async", FileAsyncPrim)
    manager.addPrimitive("url"       , URLPrim)
    manager.addPrimitive("url-async" , URLAsyncPrim)
  }

  private object FilePrim extends Reporter {
    override def getSyntax = Syntax.reporterSyntax(right = List(Syntax.StringType), ret = Syntax.StringType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val path = Paths.get(args(0).getString)
      slurp(Files.probeContentType(path)) { Files.readAllBytes(path) }
    }
  }

  private object FileAsyncPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType, Syntax.CommandType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val path     = Paths.get(args(0).getString)
      val command  = args(1).getCommand
      val contents = slurp(Files.probeContentType(path)) { Files.readAllBytes(path) }
      command.perform(context, Array(contents))
    }
  }

  private object URLPrim extends Reporter {
    override def getSyntax = Syntax.reporterSyntax(right = List(Syntax.StringType), ret = Syntax.StringType)
    override def report(args: Array[Argument], context: Context): AnyRef = {
      val url = new URL(args(0).getString)
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
      val url      = new URL(args(0).getString)
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

  private def using[T <: { def close(): Unit }, U](t: T)(f: (T) => U): U =
    try { f(t) } finally { t.close() }

  private def slurp(mimeType: String)(f: => Array[Byte]): String = {

    val bytes =
      try f
      catch {
        case ex: NoSuchFileException =>
          throw new ExtensionException(s"File not found: ${ex.getMessage}", ex)
        case ex: IOException =>
          throw new ExtensionException(s"Unable to fetch: ${ex.getMessage}", ex)
      }

    val str = new String(bytes, "UTF-8")

    if (bytes.sameElements(str.getBytes()))
      str
    else {
      s"data:$mimeType;base64,${Base64.getEncoder().encodeToString(bytes)}"
    }

  }

}
