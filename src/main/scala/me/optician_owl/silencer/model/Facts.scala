package me.optician_owl.silencer.model

import info.mukel.telegrambot4s.models.Chat

case class Facts(userStats: UserStats, evidences: List[Evidence], chat: Chat)
