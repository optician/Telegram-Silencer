package me.optician_owl.silencer.services

import com.typesafe.scalalogging.StrictLogging
import me.optician_owl.silencer.utils.Host

class ChatSettingsService extends StrictLogging {

  // ToDo Trie
  private val hardCodeWhiteList = List(
    "github.com",
    "*.github.io",
    "twitter.com",
    "pastebin.com",
    "habrahabr.ru",
    "*.jetbrains.com",
    "jetbrains.com",
    "highloadcup.ru",
    "*.scala-lang.org",
    "scala-lang.org",
    "scalafiddle.io",
    "hackage.haskell.org",
    "haskell.org",
    "scala-sbt.org",
    "rustup.rs",
    "*.rust-lang.org",
    "rust-lang.org",
  ).map(url => Host(url.toLowerCase()))

  def getExclusionUrls(chatId: Long): List[Host] = hardCodeWhiteList
}
