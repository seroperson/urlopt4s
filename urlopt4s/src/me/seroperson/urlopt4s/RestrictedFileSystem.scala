package me.seroperson.urlopt4s

import java.{util => ju}
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.AccessMode
import java.nio.file.DirectoryStream
import java.nio.file.DirectoryStream.Filter
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.attribute.FileAttribute
import org.graalvm.polyglot.io.FileSystem
import scala.collection.JavaConverters._

private[urlopt4s] class RestrictedFileSystem(
  allowedContent: Map[Path, () => SeekableByteChannel]
) extends FileSystem {

  private val delegate: FileSystem = FileSystem
    .newReadOnlyFileSystem(FileSystem.newDefaultFileSystem())

  override def parsePath(uri: URI): Path = delegate.parsePath(uri)

  override def parsePath(path: String): Path = allowedContent.find {
    case (element, _) => element.toAbsolutePath.toString == path
  } match {
    case Some((path, _)) => path
    case None => delegate.parsePath(path)
  }

  override def checkAccess(
    path: Path,
    modes: ju.Set[_ <: AccessMode],
    linkOptions: LinkOption*
  ): Unit = allowedContent.get(path) match {
    case Some(_) => ()
    case None => delegate.checkAccess(path, modes, linkOptions: _*)
  }

  override def createDirectory(dir: Path, attrs: FileAttribute[_]*): Unit =
    delegate.createDirectory(dir, attrs: _*)

  override def delete(path: Path): Unit = delegate.delete(path)

  override def newByteChannel(
    path: Path,
    options: ju.Set[_ <: OpenOption],
    attrs: FileAttribute[_]*
  ): SeekableByteChannel = allowedContent.find {
    case (element, _) => element == path
  } match {
    case Some((path, f)) => f()
    case None =>
      throw new UnsupportedOperationException(s"Unable to access $path")
  }

  override def newDirectoryStream(
    dir: Path,
    filter: Filter[_ >: Path]
  ): DirectoryStream[Path] = delegate.newDirectoryStream(dir, filter)

  override def toAbsolutePath(path: Path): Path = delegate.toAbsolutePath(path)

  override def toRealPath(path: Path, linkOptions: LinkOption*): Path = delegate
    .toRealPath(path, linkOptions: _*)

  override def readAttributes(
    path: Path,
    attributes: String,
    options: LinkOption*
  ): ju.Map[String, Object] = allowedContent.find {
    case (element, _) => element == path
  } match {
    case Some((path, f)) => Map[String, Object](
        "isRegularFile" -> Boolean.box(true),
        "isDirectory" -> Boolean.box(false)
      ).asJava

    case None => delegate.readAttributes(path, attributes, options: _*)
  }

}
