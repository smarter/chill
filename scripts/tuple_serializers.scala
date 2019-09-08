/*
 * Run this script with: scala tuple_serializer.scala
 * and it will generate the Kryo serializers for all Scala tuples
 */


import java.util.Date
import java.text.SimpleDateFormat

val header = """/*
Copyright 2012 Twitter, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.twitter.chill;

import scala.*;
import java.io.Serializable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.Serializer;

// DO NOT EDIT: auto generated by tuple_serializers.scala at: %s
// scala scripts/tuple_serializers.scala > chill-scala/src/main/java/com/twitter/chill/ScalaTupleSerialization.java
"""

def timestamp : String = {
  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z")
  dateFormat.format(new Date)
}

// Returns a string like A,B,C
// for use inside tuple type parameters
def typeList(i : Int, initC : Char = 'A') = {
  val init = initC.toInt
  (0 until i).map { idx => (idx + init).toChar }.mkString(",")
}

// Returns Tuple2[A,B] or Tuple3[A,B,C]
def tupleType(i : Int, initC : Char = 'A') = {
  "Tuple%d<%s>".format(i, typeList(i, initC))
}

def makeWrite(size : Int) : String = {
  val head = """  public void write(Kryo kser, Output out, %s obj) {
""".format(tupleType(size))
  val unrolled = (1 to size).map { i =>
  "    kser.writeClassAndObject(out, obj._%d()); out.flush();".format(i)
    }.mkString("\n")
  head + unrolled + "\n  }\n"
}

def readAndCast(pos : Int) : String = {
  """      (%s) kser.readClassAndObject(in)""".format(('A'.toInt + pos - 1).toChar.toString)
}

def makeRead(size : Int) : String = {
  val ttype = tupleType(size)
  val head = """  public %s read(Kryo kser, Input in, Class<%s> cls) {
""".format(ttype, ttype)
  val instantiation = """    return new %s(
""".format(ttype)
  val reads = (1 to size).map { readAndCast(_) }.mkString(",\n")
  head + instantiation + reads + """
    );
  }
"""
}

def makeSerializer(size : Int) : String = {
  val tList = typeList(size)
  val tType = tupleType(size)
  ("""class Tuple%dSerializer<%s> extends Serializer<%s> implements Serializable {
  public Tuple%dSerializer() {
    setImmutable(true);
  }
""".format(size,tList,tType,size,tList)) + makeWrite(size) + makeRead(size) + "}"
}

def register(size : Int) : String = {
  val anyArgs = (1 to size).map { i => "Object" }.mkString(",")
  val ttype = "Tuple%d".format(size)
  val ser = "new Tuple%dSerializer<%s>()".format(size, anyArgs)
  """    newK.register(%s.class, %s);""".format(ttype, ser)
}

val typeMap = Map("Long" -> "J", "Int" -> "I", "Double" -> "D")
val spTypes = List("Long", "Int", "Double")

val spPairs = for(a <- spTypes; b <- spTypes) yield (a,b)

def spTup1(typeNm : String) : String = {
"""
class Tuple1TYPESerializer extends Serializer<Tuple1$mcSHORT$sp> implements Serializable {
  public Tuple1TYPESerializer() {
    setImmutable(true);
  }
  public Tuple1$mcSHORT$sp read(Kryo kser, Input in, Class<Tuple1$mcSHORT$sp> cls) {
    return new Tuple1$mcSHORT$sp(in.readTYPE());
  }
  public void write(Kryo kser, Output out, Tuple1$mcSHORT$sp tup) {
    out.writeTYPE(tup._1$mcSHORT$sp);
  }
}

""".replace("TYPE", typeNm).replace("SHORT", typeMap(typeNm))
}

def spTup2(typeNm1 : String, typeNm2 : String) : String = {
"""
class Tuple2TYPE1TYPE2Serializer extends Serializer<Tuple2$mcSHORT1SHORT2$sp> implements Serializable {
  public Tuple2TYPE1TYPE2Serializer() {
    setImmutable(true);
  }
  public Tuple2$mcSHORT1SHORT2$sp read(Kryo kser, Input in, Class<Tuple2$mcSHORT1SHORT2$sp> cls) {
    return new Tuple2$mcSHORT1SHORT2$sp(in.readTYPE1(), in.readTYPE2());
  }
  public void write(Kryo kser, Output out, Tuple2$mcSHORT1SHORT2$sp tup) {
    out.writeTYPE1(tup._1$mcSHORT1$sp);
    out.writeTYPE2(tup._2$mcSHORT2$sp);
  }
}
""".replace("TYPE1", typeNm1)
  .replace("SHORT1", typeMap(typeNm1))
  .replace("TYPE2", typeNm2)
  .replace("SHORT2", typeMap(typeNm2))
}

def registerSp1(typeNm : String) : String = {
  """    newK.register(Tuple1$mcSHORT$sp.class, new Tuple1TYPESerializer());"""
    .replace("TYPE", typeNm).replace("SHORT", typeMap(typeNm))
}

def registerSp2(typeNm1 : String, typeNm2 : String) : String = {
  """    newK.register(Tuple2$mcSHORT1SHORT2$sp.class, new Tuple2TYPE1TYPE2Serializer());"""
  .replace("TYPE1", typeNm1)
  .replace("SHORT1", typeMap(typeNm1))
  .replace("TYPE2", typeNm2)
  .replace("SHORT2", typeMap(typeNm2))
}

val objectHelper : String = {
"""public class ScalaTupleSerialization implements Serializable {
  public static IKryoRegistrar register() { return new IKryoRegistrar() {
    public void apply(Kryo newK) {

""" + ((1 to 22).map { size => register(size) }.mkString("\n")) + "\n" +
  (spTypes.map { registerSp1(_) }.mkString("\n")) + "\n" +
  (spPairs.map { t => registerSp2(t._1, t._2) }.mkString("\n")) + "\n" +
"""    }
  }; }
}
"""
}

///////////////////////////////////////////////////////////////////
// Actually output the code here:
println(header.format(timestamp))
(1 to 22).foreach { idx => println( makeSerializer(idx) ) }
spTypes.foreach { t => println(spTup1(t)) }
spPairs.foreach { t => println(spTup2(t._1, t._2)) }

print(objectHelper)
