package com.appasadito.AsaditoRg;

import com.appasadito.AsaditoRg.modelos.Locales;
import com.appasadito.AsaditoRg.modelos.Precios;
import com.appasadito.AsaditoRg.modelos.Productos;
import com.appasadito.AsaditoRg.repositorios.LocalesRepository;
import com.appasadito.AsaditoRg.repositorios.PreciosRepository;
import com.appasadito.AsaditoRg.repositorios.ProductosRepository;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication

public class AsaditoRgApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsaditoRgApplication.class, args);

	}

	@Bean
	public CommandLineRunner initData(LocalesRepository localrepository, ProductosRepository productorepository, PreciosRepository preciorepository){
		return (args) -> {
			if(productorepository.findAll().isEmpty()){
				String[] nombre_productos = {
						"Tapa de Asado x Kg.",
						"Vacio x Kg.",
						"Asado x Kg.",
						"Molleja x Kg.",
						"Matambre x Kg.",
						"Salchicha Parrillera x Kg.",
						"Chorizo Bombón de Carne x Kg.",
						"Chinchulin x Kg.",
				};

				for (String nombre : nombre_productos){
					Productos producto = new Productos(nombre);
					productorepository.save(producto);
				}
			}

			if(localrepository.findAll().isEmpty()){

				String[][] listado_locales = {
						{"Mercado Concentrador de la Costa", "Islas Malvinas 734", "-53.77954", "-67.71504"},
						{"Carrefour", "Av. San Martin 685", "-53.78528", "-67.70340"},
						{"Alta Performance", "Comandante Tomas Espora 799", "-53.78901", "-67.69925"},
						{"El Príncipe", "Perito Moreno 99", "-53.79047", "-67.69615"},
						{"Sur Fruit SRL", "9 de Julio 936", "-53.79064", "-67.69958"},
						{"Trelew", "J. M. Estrada 803", "-53.78548", "-67.70625"},
						{"Mercadito El Jassmin", "Geronimo Vukasovic 2885", "-53.77171", "-67.73459"},
						{"El Monumental", "Islas Malvinas 2843", "-53.78841", "-67.72614"},
						{"El Roble", "Carlos Pellegrini 158", "-53.77263", "-67.72273"},
						{"El Puente", "Provincias Unidas 898", "-53.76512", "-67.72446"},
						{"La Anonima", "Av. San Martin 1678", "-53.77872", "-67.71660"},
						{"Los Flamencos", "Rio Grande 3162", "-53.79717", "-67.75510"},
						{"El Novillito", "Enrique Arteche 1069", "-53.79853", "-67.68717"},
				};

				for (String[] local : listado_locales){
					Locales localobj = new Locales(local[0],local[1],local[2],local[3]);
					localrepository.save(localobj);
				}
			}
		};
	}

}
