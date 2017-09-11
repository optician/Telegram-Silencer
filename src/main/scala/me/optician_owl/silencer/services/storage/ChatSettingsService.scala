package me.optician_owl.silencer.services.storage

import com.typesafe.scalalogging.StrictLogging
import me.optician_owl.silencer.utils.Host

class ChatSettingsService extends StrictLogging {

  // ToDo Trie
  private val hardCodeWhiteList = List(
    "github.com",
    "*.github.com",
    "*.github.io",
    "twitter.com",
    "pastebin.com",
    "google.com",
    "google.ru",
    "yandex.ru",
    "clickhouse.yandex",
    "habrahabr.ru",
    "*.apache.org",
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
    "stackoverflow.com",
    "*.stackoverflow.com",
    "serverfault.com",
    "*.serverfault.com",
    "superuser.com",
    "*.superuser.com",
    "stackexchange.com",
    "*.stackexchange.com",
    "android.com",
    "*.android.com",
  ).map(url => Host(url.toLowerCase()))

  def getExclusionUrls(chatId: Long): List[Host] = hardCodeWhiteList
}
