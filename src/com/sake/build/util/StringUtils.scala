package com.sake.build.util

object StringUtils {

  def trim(str: String) = str.trim().replaceAll("\n", "").replaceAll("\r", "")
}