package org.botellier.protocol.command

import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

@Target(AnnotationTarget.CLASS)
annotation class WithCommand(val name: String)

@Target(AnnotationTarget.FIELD)
annotation class Parameter(val order: Int, val optional: Boolean = false)

abstract class Command {

    val name: String by lazy {
        val withCommand = this::class.annotations.find { it is WithCommand } as? WithCommand
        withCommand?.name?.toUpperCase() ?: throw InvalidCommandException()
    }

    val parameters by lazy {
        this::class.declaredMemberProperties
                .filter {
                    val field = this::class.java.getDeclaredField(it.name)

                    if (field.javaClass.isInstance(CValue::class)) {
                        throw InvalidPropertyException(this::class.toString(), it.name, "must be a CValue")
                    }
                    if (it !is KMutableProperty<*>) {
                        throw InvalidPropertyException(this::class.toString(), it.name, "must be mutable")
                    }

                    field.getAnnotation(Parameter::class.java) != null
                }
                .filterIsInstance<KMutableProperty<CValue>>()
                .sortedBy {
                    val field = this::class.java.getDeclaredField(it.name)
                    val parameter = field.getAnnotation(Parameter::class.java)
                    parameter?.order ?: 0
                }
                .map {
                    val field = this::class.java.getDeclaredField(it.name)
                    val parameter = field.getAnnotation(Parameter::class.java)
                    CParameter(this, it, parameter?.optional ?: false)
                }
    }


    // Values for initializing parameters in child classes.
    protected val intValue = CValue.Primitive.Int(0)
    protected val floatValue = CValue.Primitive.Float(0.0)
    protected val stringValue = CValue.Primitive.String("")
    protected val anyValue: CValue.Primitive = CValue.Primitive.Any()

    protected val intArrayValue = CValue.Array.Int(emptyArray())
    protected val floatArrayValue = CValue.Array.Float(emptyArray())
    protected val stringArrayValue = CValue.Array.String(emptyArray())
    protected val anyArrayValue = CValue.Array.Any(emptyArray())

    // Other.
    override fun toString(): String {
        val builder = StringBuilder()
        var it = parameters.iterator()
        while (it.hasNext()) {
            builder.append(it.next().get().toString())
            if (it.hasNext()) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }
}

class InvalidCommandException : Throwable("Command must declare a name using the @WithCommand annotation")
class InvalidPropertyException(className: String, paramName: String, message: String)
    : Throwable("Property '$paramName' from [$className]: $message")
class InvalidPrimitiveException(value: Any) : Throwable("Cannot construct a command primitive using $value.")
