Nexus Mobile es una plataforma de tienda de videojuegos moderna para Android, construida con Jetpack Compose y Firebase. Permite explorar, comprar y gestionar una biblioteca de juegos, además de interactuar con una comunidad de jugadores.


 Tienda de Juegos

Catálogo completo de videojuegos con categorías
Búsqueda y filtrado avanzado
Detalles completos de cada juego (capturas, descripción, reseñas)
Sistema de reseñas con calificaciones

 Carrito y Checkout

Carrito de compras en tiempo real
Simulación de pago (tarjeta de crédito / Yape)
Historial de órdenes

 Biblioteca Personal

Gestión de juegos adquiridos
Visualización de biblioteca personal

 Comunidad

Crear y compartir publicaciones
Subir imágenes en posts
Sistema de likes y comentarios
Feed en tiempo real

🤖 Chatbot con IA

Asistente virtual con Groq AI (Llama 3.1)
Recomendaciones personalizadas de juegos
Conversación natural

 Perfil de Usuario

Edición de perfil con foto
Subida de imágenes a Cloudinary
Gestión de privacidad y seguridad
Cambio de contraseña


 Tecnologías
Frontend

Kotlin - Lenguaje principal
Jetpack Compose - UI moderna y declarativa
Material Design 3 - Componentes UI
Coil - Carga de imágenes
Navigation Compose - Navegación entre pantallas

Backend

Firebase Authentication - Autenticación de usuarios
Cloud Firestore - Base de datos NoSQL en tiempo real
Firebase Storage - Almacenamiento de archivos

APIs y Servicios

Groq AI - Chatbot con LLM (Llama 3.1)
Cloudinary - Gestión de imágenes
OkHttp - Cliente HTTP

Arquitectura

MVVM (Model-View-ViewModel)
Clean Architecture - Separación en capas (domain, data, presentation)
Repository Pattern - Abstracción de fuentes de datos
Kotlin Coroutines - Programación asíncrona
StateFlow - Gestión de estado reactivo


Arquitectura
app/
├── data/                          # Capa de Datos
│   ├── api/                       # Servicios externos (Groq)
│   ├── repository/                # Implementación de repositorios
│   └── model/                     # DTOs
│
├── domain/                        # Capa de Dominio
│   ├── model/                     # Modelos de negocio
│   ├── repository/                # Interfaces de repositorios
│   └── usecase/                   # Casos de uso
│
└── presentation/                  # Capa de Presentación
    ├── ui/                        # Pantallas Compose
    │   ├── auth/                  # Login y Registro
    │   ├── home/                  # Tienda principal
    │   ├── game/                  # Detalle de juegos
    │   ├── cart/                  # Carrito de compras
    │   ├── checkout/              # Proceso de pago
    │   ├── library/               # Biblioteca de juegos
    │   ├── community/             # Red social
    │   ├── chatbot/               # Asistente IA
    │   └── profile/               # Perfil de usuario
    └── viewmodel/                 # ViewModels (lógica UI)
Patrón MVVM:

Model: Modelos de datos y repositorios
View: Composables de Jetpack Compose
ViewModel: Lógica de presentación y gestión de estado


Requisitos Previos

Android Studio Hedgehog (2023.1.1) o superior
JDK 11 o superior
Android SDK API 26+ (Android 8.0+)
Cuenta de Firebase (gratuita)
Cuenta de Cloudinary (gratuita)
API Key de Groq (gratuita - groq.com)

