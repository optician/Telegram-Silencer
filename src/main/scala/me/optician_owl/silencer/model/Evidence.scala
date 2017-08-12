package me.optician_owl.silencer.model

sealed trait Evidence
object OuterLink    extends Evidence
object TelegramLink extends Evidence
