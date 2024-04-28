package br.com.carrefour.catalogo.core.mapper

import com.github.dozermapper.core.DozerBeanMapperBuilder
import com.github.dozermapper.core.Mapper

object DozerMapper {

    private val mapper: Mapper = DozerBeanMapperBuilder.buildDefault()

    fun<O, D> parseObject(origin: O, destination: Class<D>?): D = mapper.map(origin, destination)

    fun<O, D : Any> parseListObject(origin: List<O>, destination: Class<D>?): ArrayList<D> {
        val destinationObjects = ArrayList<D>()

        for (o in origin) destinationObjects.add(mapper.map(o, destination))

        return destinationObjects
    }

}