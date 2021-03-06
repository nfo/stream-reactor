/**
  * Copyright 2016 Datamountaineer.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  **/

package com.datamountaineer.streamreactor.connect.mongodb.sink

import java.util

import com.datamountaineer.streamreactor.connect.mongodb.config.MongoConfig
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

/**
  * <h1>MongoSinkTask</h1>
  *
  * Kafka Connect Mongo DB sink task. Called by
  * framework to put records to the target sink
  **/
class MongoSinkTask extends SinkTask with StrictLogging {
  private var writer: Option[MongoWriter] = None

  logger.info("Task initialising")

  /**
    * Parse the configurations and setup the writer
    **/
  override def start(props: util.Map[String, String]): Unit = {
    val taskConfig = Try(MongoConfig(props)) match {
      case Failure(f) => throw new ConnectException("Couldn't start Mongo Sink due to configuration error.", f)
      case Success(s) => s
    }

    logger.info(
      """
        |  ____        _        __  __                   _        _
        | |  _ \  __ _| |_ __ _|  \/  | ___  _   _ _ __ | |_ __ _(_)_ __   ___  ___ _ __
        | | | | |/ _` | __/ _` | |\/| |/ _ \| | | | '_ \| __/ _` | | '_ \ / _ \/ _ \ '__|
        | | |_| | (_| | || (_| | |  | | (_) | |_| | | | | || (_| | | | | |  __/  __/ |
        | |____/ \__,_|\__\__,_|_|  |_|\___/ \__,_|_| |_|\__\__,_|_|_| |_|\___|\___|_|
        |  __  __                         ____  _       ____  _       _ by Stefan Bocutiu
        | |  \/  | ___  _ __   __ _  ___ |  _ \| |__   / ___|(_)_ __ | | __
        | | |\/| |/ _ \| '_ \ / _` |/ _ \| | | | '_ \  \___ \| | '_ \| |/ /
        | | |  | | (_) | | | | (_| | (_) | |_| | |_) |  ___) | | | | |   <
        | |_|  |_|\___/|_| |_|\__, |\___/|____/|_.__/  |____/|_|_| |_|_|\_\
        |.""".stripMargin)

    writer = Some(MongoWriter(taskConfig, context = context))
  }

  /**
    * Pass the SinkRecords to the mongo db writer for storing them
    **/
  override def put(records: util.Collection[SinkRecord]): Unit = {
    require(writer.nonEmpty, "Writer is not set!")
    writer.foreach(w => w.write(records.toVector))
  }

  override def stop(): Unit = {
    logger.info("Stopping Mongo Database sink.")
    writer.foreach(w => w.close())
  }

  override def flush(map: util.Map[TopicPartition, OffsetAndMetadata]): Unit = {}

  override def version(): String = getClass.getPackage.getImplementationVersion
}
