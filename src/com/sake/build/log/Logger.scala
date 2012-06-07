package com.sake.build.log

import java.util.Date

trait Logger {

  def info(msg: String) {
    println("[INFO] "+new Date()+" - "+msg)
  }

  def info_nolinebreak(msg: String) {
    print("[INFO] "+new Date()+" - "+msg)
  }

  def debug(msg: String) {
    println("[DEBUG] "+new Date()+" - "+msg)
  }

  def error(msg: String) {
    println("[ERROR] "+new Date()+" - "+msg)
  }
}
