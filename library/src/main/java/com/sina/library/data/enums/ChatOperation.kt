package com.sina.library.data.enums

import com.sina.library.utility.R


enum class ChatOperation(override val icon: String, override val resNameId: Int) : OperationItem {
    REPLY("f3dd", R.string.txt_reply),
    FORWARD("f3b6", R.string.txt_forward),
    DELETE("f273", R.string.txt_delete),
    EDIT("f022", R.string.txt_edit),
    COPY("f2ad", R.string.txt_copy),
}