package me.optician_owl.silencer.model

sealed trait CallbackCommand {
  def cmd: String
}

object DeleteMsgAndBan extends CallbackCommand {
  override val cmd: String = "delete-n-ban"
}

object DeleteMsg extends CallbackCommand {
  override val cmd: String = "delete"
}

object Forgive extends CallbackCommand {
  override val cmd: String = "forgive"
}
