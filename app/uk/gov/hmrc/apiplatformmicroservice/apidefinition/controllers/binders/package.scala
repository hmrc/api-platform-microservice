/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment

package object binders {
  import play.api.mvc.{PathBindable, QueryStringBindable}
  import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

  implicit def environmentPathBinder(implicit textBinder: PathBindable[String]): PathBindable[Environment] = new PathBindable[Environment] {

    override def bind(key: String, value: String): Either[String, Environment] = {
      for {
        text <- textBinder.bind(key, value)
        env  <- Environment.from(text).toRight("Not a valid environment")
      } yield env
    }

    override def unbind(key: String, env: Environment): String = {
      env.toString.toLowerCase
    }
  }

  implicit def apiContextPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiContext] = new PathBindable[ApiContext] {

    override def bind(key: String, value: String): Either[String, ApiContext] = {
      textBinder.bind(key, value).map(ApiContext(_))
    }

    override def unbind(key: String, apiContext: ApiContext): String = {
      apiContext.value
    }
  }

  implicit def apiContextQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiContext] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiContext]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(context) => Right(ApiContext(context))
          case _              => Left("Unable to bind an api context")
        }
      }
    }

    override def unbind(key: String, context: ApiContext): String = {
      textBinder.unbind(key, context.value)
    }
  }

  implicit def apiVersionPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiVersionNbr] = new PathBindable[ApiVersionNbr] {

    override def bind(key: String, value: String): Either[String, ApiVersionNbr] = {
      textBinder.bind(key, value).map(ApiVersionNbr(_))
    }

    override def unbind(key: String, apiVersion: ApiVersionNbr): String = {
      apiVersion.value
    }
  }

  implicit def apiVersionQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiVersionNbr] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiVersionNbr]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(version) => Right(ApiVersionNbr(version))
          case _              => Left("Unable to bind an api version")
        }
      }
    }

    override def unbind(key: String, version: ApiVersionNbr): String = {
      textBinder.unbind(key, version.value)
    }
  }
}
