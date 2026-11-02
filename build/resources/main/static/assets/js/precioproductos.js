Vue.createApp({
    data() {
        return {
            productosInfo: [],
            errorToats: null,
            errorMsg: null,
            accountToDeleteId: null,
            nombre: "",

        }
    },

    methods: {
            getData() {
                axios.get("api/precios/ultimos")
                      .then((response) => {
                        this.productosInfo = response.data;


                        this.productosInfo.forEach(store => {
                          store.productos.forEach(product => {
                            if (product.quantity === undefined || product.quantity === null) {
                              product.quantity = 0;
                            }
                          });
                        });
                      })
                      .catch((error) => {
                        console.error(error);
                      });
              },

            // Aumentar cantidad de kg
            increaseQuantity(store, product) {
                // Aseguramos que quantity sea un número antes de realizar la operación
                if (typeof product.quantity !== 'number') {
                  this.$set(product, 'quantity', 0); // Si no es un número, inicializamos a 0
                }
                product.quantity += 0.5;
              },

              // Disminuir cantidad de kg
            decreaseQuantity(store, product) {
                if (typeof product.quantity !== 'number') {
                  this.$set(product, 'quantity', 0); // Si no es un número, inicializamos a 0
                }
                if (product.quantity > 0) {
                  product.quantity -= 0.5;
                }
              },

            clearQuantities() {
                  this.productosInfo.forEach(store => {
                    store.productos.forEach(product => {
                      product.quantity = 0;  // Establece la cantidad de cada producto a 0
                    });
                  });
                },

              // Generar el PDF con jsPDF


          /*  generatePDF() {
                    const { jsPDF } = window.jspdf;
                    const doc = new jsPDF();

                    const imageUrl = "http://localhost:8080/assets/img/Logo%20Asadito%20RG%20-%20PNG.png";  // URL de tu logo

                    // Agregar la imagen (logo) al reporte
                    // Parámetros: (URL de la imagen, formato, x, y, ancho, alto)
                    doc.addImage(imageUrl, 'PNG', 10, 10, 50, 20);

                    // Título
                    doc.text("Presupuesto del Asado", 70, 25);

                    let y = 50; // Variable para la posición vertical del texto

                    // Iteramos sobre los stores y productos para generar el contenido
                    this.productosInfo.forEach(store => {
                        const selectedProducts = store.productos.filter(product => product.quantity > 0);

                        if (selectedProducts.length > 0) {
                            doc.text(store.localNombre, 10, y);
                            y += 10;

                            selectedProducts.forEach(product => {
                                doc.text(`${product.nombre}: ${product.quantity} kg x $${product.precio} = $${(product.precio * product.quantity).toFixed(2)}`, 10, y);
                                y += 10;
                            });

                            y += 10; // Espacio entre locales
                        }
                    });

                    // Si hay productos seleccionados, agregamos el total al final
                    if (this.totalPrice > 0) {
                        doc.text(`Total: $${this.totalPrice}`, 10, y);
                    }

                    // Guardar el PDF
                    doc.save("reporte_precio_cortes.pdf");
                  },*/

            generatePDF() {
                const { jsPDF } = window.jspdf;
                const doc = new jsPDF();

                const imageUrl = "http://localhost:8080/assets/img/Logo%20Asadito%20RG%20-%20PNG.png";

                // Logo
                doc.addImage(imageUrl, 'PNG', 10, 10, 50, 20);

                // Título
                doc.setFontSize(16);
                doc.text("Presupuesto del Asado", 70, 25);

                let y = 40; // Posición inicial

                this.productosInfo.forEach(store => {
                    const selectedProducts = store.productos.filter(product => product.quantity > 0);

                    if (selectedProducts.length > 0) {
                        // Nombre del local
                        doc.setFontSize(12);
                        doc.text(store.localNombre, 10, y);

                        y += 5;

                        // Crear tabla con productos
                        const tableBody = selectedProducts.map(product => [
                            product.nombre,
                            `${product.quantity} kg`,
                            `$${product.precio}`,
                            `$${(product.precio * product.quantity).toFixed(2)}`
                        ]);

                        doc.autoTable({
                            startY: y + 5,
                            head: [['Producto', 'Cantidad', 'Precio unitario', 'Subtotal']],
                            body: tableBody,
                            theme: 'grid',
                            styles: { fontSize: 10 },
                            headStyles: { fillColor: [22, 160, 133] },
                            margin: { left: 10, right: 10 },
                            didDrawPage: data => {
                                y = data.cursor.y + 10; // Actualizar Y después de la tabla
                            }
                        });
                    }
                });

                if (this.totalPrice > 0) {
                    doc.setFontSize(12);
                    doc.text(`Total: $${Number(this.totalPrice).toFixed(2)}`, 10, y);

                }

                doc.save("reporte_precio_cortes.pdf");
            },

            isProductAvailable(store, product) {
                      // Buscamos si el producto está disponible en el local (comprobando por nombre)
                      return store.productos.some(p => p.nombre === product);
                  },

            getProductPrice(store, product) {
                      // Obtenemos el precio del producto en el local
                      const productData = store.productos.find(p => p.nombre === product);
                      return productData ? productData.precio : null;
                  },

            },

    computed: {
            // Calcula el total de todos los productos seleccionados
            totalPrice() {
                      return this.productosInfo.reduce((total, store) => {
                        return total + store.productos.reduce((subTotal, product) => {
                          const quantity = product.quantity || 0;
                          return subTotal + (product.precio * quantity);
                        }, 0);
                      }, 0).toFixed(2); // Formato de dos decimales para el total
                    },

            productos() {
                        // Extraemos una lista de todos los productos únicos que tenemos en los locales
                        const productosSet = new Set();
                                    this.productosInfo.forEach(store => {
                                        store.productos.forEach(product => {
                                            productosSet.add(product.nombre);
                                        });
                                    });
                                    return Array.from(productosSet);
                    }
          },

    mounted: function () {
        this.getData();
    }
}).mount('#app')