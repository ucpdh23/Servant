package es.xan.servantv3.data

import java.io.Serializable

data class Type(var type : String)



data class When(var type : Type)
data class Where(var type : Type)
data class Who(var type : Type)
data class What(var type : Type)
data class Why(var type : Type)
data class How(var type : Type)


data class Fact(var message: String, var type: Type, var _when: When, var _where: Where, var _who: Who, var _what: What, var _why: Why, var _how: How)
