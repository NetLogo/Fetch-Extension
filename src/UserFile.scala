package org.nlogo.extension.fetch

import java.awt.{ FileDialog => JFileDialog }
import java.io.File
import java.lang.{ Boolean => JBoolean }

import org.nlogo.api.{ ExtensionException, ReporterRunnable, Workspace }
import org.nlogo.awt.{ EventQueue, UserCancelException }
import org.nlogo.nvm.HaltException
import org.nlogo.swing.FileDialog
import org.nlogo.window.GUIWorkspace

// The code here is adapted from 'prim/gui/_userfile' --JAB (3/11/19)
object UserFile {

  def userFileAsync(workspace: Workspace)(callback: (AnyRef) => Unit): Unit = {

    withGUIWorkspace(workspace) {

      gw =>

        gw.updateUI()

        EventQueue.invokeLater(new Runnable() {
          def run() {

            val result =
              try {
                gw.view.mouseDown(false)
                FileDialog.setDirectory(gw.fileManager.prefix)
                FileDialog.showFiles(gw.getFrame, "Choose File", JFileDialog.LOAD)
              } catch {
                case _: UserCancelException => JBoolean.FALSE
              }

            handleResult(result).fold(callback, callback)

          }
        })

    }

  }

  def userFileSync(workspace: Workspace): Either[JBoolean, String] = {

    var result: AnyRef = null

    withGUIWorkspace(workspace) {

      gw =>

        gw.updateUI()

        result =
          gw.waitForResult(
            new ReporterRunnable[AnyRef] {
              override def run() =
                try {
                  gw.view.mouseDown(false)
                  FileDialog.setDirectory(gw.fileManager.prefix)
                  FileDialog.showFiles(gw.getFrame, "Choose File", JFileDialog.LOAD)
                } catch {
                  case _: UserCancelException => JBoolean.FALSE
                }
            }
          )

    }

    handleResult(result)

  }

  private def withGUIWorkspace(workspace: Workspace)(action: (GUIWorkspace) => Unit): Unit =
    workspace match {
      case gw: GUIWorkspace => action(gw)
      case _                => throw new ExtensionException("You can't get user input headlessly.")
    }

  private def handleResult(result: AnyRef): Either[JBoolean, String] =
    result match {
      case null =>
        throw new HaltException(false)
      case b: JBoolean =>
        Left(b)
      case s: String =>
        if (!new File(s).exists)
          throw new ExtensionException("This file doesn't exist.")
        else
          Right(s)
    }

}
