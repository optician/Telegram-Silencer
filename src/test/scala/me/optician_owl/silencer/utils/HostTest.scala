package me.optician_owl.silencer.utils

import org.scalatest.{FlatSpec, Matchers}

class HostTest extends FlatSpec with Matchers {

  behavior of "HostTest"

  it should "have correct Ordering" in {
    val host1 = Host("github.com")
    val host2 = Host("some.github.com")
    val host3 = Host("some.github.com")
    val host4 = Host("some.github.io")
    val host5 = Host("*.github.io")
    val host6 = Host("some.*.com")

    val ord = implicitly[Ordering[Host]]

    ord.compare(host1, host2) should be(-1)
    ord.compare(host2, host3) should be(0)
    ord.compare(host4, host3) should be(1)
    ord.compare(host4, host5) should be(0)
    ord.compare(host5, host6) should be(1)
    ord.compare(host3, host6) should be(0)

  }

  it should "correctly check telegram links" in {
    val url1   = Host.fromUrl("https://t.me/somebody")
    val url1_1 = Host.fromUrl("t.me/somebody")
    val url2   = Host.fromUrl("https://me.me/somebody")
    val url2_1 = Host.fromUrl("me.me/somebody")
    val url3   = Host.fromUrl("https://t.it/somebody")
    val url4   = Host.fromUrl("https://telegram.me/somebody")
    val url5   = Host.fromUrl("https://telegram.com/somebody")

    assert(url1.exists(_.isTelegram))
    assert(url1_1.exists(_.isTelegram))
    assert(url2.exists(x => !x.isTelegram))
    assert(url2_1.exists(x => !x.isTelegram))
    assert(url3.exists(x => !x.isTelegram))
    assert(url4.exists(_.isTelegram))
    assert(url5.exists(x => !x.isTelegram))
  }

}
