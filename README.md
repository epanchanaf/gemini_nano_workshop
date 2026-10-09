# Taller de Gemini Nano 🚀

Aplicación Android de demostración para explorar capacidades de IA en el dispositivo con **ML Kit**. Las tareas generativas usan **Gemini Nano** mediante AI Core; el dictado usa el modo Basic de reconocimiento de voz local. La interfaz está en español y permite probar tareas de texto, imagen y voz sin enviar el contenido a un servicio de generación en la nube.

> El modelo o las funciones de ML Kit pueden requerir una descarga inicial desde el dispositivo compatible. La app muestra el estado de disponibilidad y el progreso de descarga antes de habilitar cada función.

## Funcionalidades

- **Resumen:** convierte un artículo o conversación en un resumen de tres viñetas.
- **Corrección:** detecta y corrige errores en mensajes cortos.
- **Reescritura:** transforma un texto para reformularlo, acortarlo, ampliarlo o darle un tono amigable o profesional.
- **Descripción de imágenes:** selecciona una imagen del dispositivo y genera una descripción breve y objetiva.
- **Chat:** acepta texto o una imagen con una pregunta. Las respuestas a texto llegan progresivamente; las respuestas a imágenes aparecen al completarse.
- **Reconocimiento de voz:** transcribe el micrófono en español y muestra resultados parciales durante el dictado.
- **Diario por voz:** crea entradas locales a partir de un dictado, aplica colores según el tono de la vivencia y acepta órdenes habladas para eliminar una entrada o cambiar la paleta de la app. El borrado requiere confirmación.
- **Estado del modelo:** consulta la disponibilidad de AI Core y de las funciones GenAI necesarias antes de usarlas.

## Tecnologías

- Kotlin y Jetpack Compose con Material 3
- Navigation Compose para la navegación entre pantallas
- Hilt para inyección de dependencias
- Kotlin Coroutines y Flow para operaciones asíncronas y respuestas en streaming
- ML Kit GenAI: Summarization, Proofreading, Rewriting, Image Description, Prompt/Generation y Speech Recognition
- Gemini Nano a través de AI Core para las tareas generativas; Speech Recognition en modo Basic para dictado local
- Room para las entradas y preferencias del diario; renderer A2UI oficial para mostrar cada entrada con componentes Compose Android

## Arquitectura de la app

La aplicación emplea una arquitectura **MVVM** orientada a estado. Cada funcionalidad mantiene su propia pantalla, `ViewModel`, estado de UI y repositorio; la navegación conecta esas pantallas desde un único `NavHost`.

```text
Usuario
   │
   ▼
Pantallas Compose ──► ViewModel ──► Repositorio ──► ML Kit GenAI ──► AICore ──► Gemini Nano
   ▲                     │                │                              │
   └──── StateFlow ◄─────┴──── resultado, errores y progreso ─────────────┘
```

| Capa | Responsabilidad | Ubicación |
| --- | --- | --- |
| Presentación | Pantallas Compose, componentes reutilizables, tema y navegación. | `ui/` |
| Estado | Recibe las acciones del usuario, expone `StateFlow`, ejecuta corrutinas y adapta el progreso o los errores para la interfaz. | `ui/home/**/**ViewModel.kt` |
| Datos/IA | Crea los clientes de ML Kit, comprueba disponibilidad, descarga componentes y solicita la inferencia. | `data/genai/repository/` |
| Plataforma | `AICore` ejecuta y administra el modelo Gemini Nano disponible en el dispositivo. | Servicio de Android |

**Flujo de una capacidad:** la pantalla envía una acción al `ViewModel`; este consulta el estado de la función en su repositorio. Si el componente está disponible, ejecuta la inferencia; si es descargable, observa el progreso antes de continuar; si no es compatible, entrega un mensaje de error a la UI. Los repositorios se cierran al finalizar el ciclo de vida de su `ViewModel` para liberar los clientes de IA.

Hilt proporciona los repositorios a los `ViewModel`, y `Navigation Compose` separa las rutas de resumen, corrección, reescritura, descripción de imágenes, chat y reconocimiento de voz. El chat usa Prompt API: las solicitudes de texto llegan en *streaming*, mientras que las de imagen envían `ImagePart` y `TextPart` juntos y muestran la respuesta al completarse. La pantalla independiente de descripción de imágenes sigue usando la API especializada `ImageDescriber`.

Para probar el chat multimodal, abre **Chat**, toca el botón de imagen, elige una foto y envíala con una pregunta. También puedes enviarla sin escribir: se utilizará una instrucción breve para describirla. El chat conserva la imagen más reciente como contexto para preguntas posteriores. La descripción independiente usa [GenAI Image Description](https://developers.google.com/ml-kit/genai/image-description/android), que actualmente genera descripciones breves en inglés; las preguntas personalizadas sobre imágenes del chat usan [GenAI Prompt API](https://developers.google.com/ml-kit/genai/prompt/android/get-started).

Para probar el dictado, abre **Reconocimiento de voz**, concede el permiso de micrófono y pulsa **Iniciar dictado**. La implementación usa `es-ES` y `MODE_BASIC` de [GenAI Speech Recognition](https://developers.google.com/ml-kit/genai/speech-recognition/android). Este modo utiliza el reconocedor tradicional del dispositivo, no Gemini Nano; el modo Advanced basado en GenAI solo está disponible en determinados equipos. La API de voz está en fase alpha y la disponibilidad del idioma depende del dispositivo.

En **Diario por voz**, el dictado Basic transcribe la orden; Gemini Nano interpreta localmente si debe crear una entrada, identificar una entrada para borrar o cambiar el tema. La app valida esa decisión, guarda las entradas y el tema en Room, y reconstruye la vista de cada entrada como una superficie A2UI. Las órdenes de borrado ambiguas no eliminan nada. La base de datos guarda texto, no audio, y la app desactiva sus copias de seguridad.

Los repositorios reciben una `GenAiClientFactory` por constructor. Hilt la enlaza con `MlKitGenAiClientFactory` mediante `GenAiModule`; esta implementación concentra las llamadas a `getClient()` y las opciones de ML Kit. La fábrica no almacena clientes: cada repositorio conserva y cierra los que solicita. El repositorio de estado crea clientes temporales por consulta y los cierra al terminar. En pruebas se puede pasar otra implementación de la fábrica para sustituir los clientes sin inicializar AICore.

El contrato de errores de datos es `AiException`: distingue entrada inválida, modelo no disponible, fallo de descarga, resultado vacío y fallos de consulta o generación. Los repositorios validan las entradas y las respuestas finales, y conservan la excepción original como causa para diagnóstico. En streaming, los fragmentos vacíos son válidos; terminar sin texto útil produce `EmptyResult`. Las cancelaciones se propagan como `CancellationException`. Cada `ViewModel` usa el mismo mapeador de mensajes de UI, sin mostrar los mensajes internos del SDK. El método `prepare()` de los repositorios consulta disponibilidad y descarga cuando es necesario; si ya hay una descarga en curso, permite informar al usuario y reintentar.

Cada operación de usuario expone un `Flow<AiOperationState<T>>`. El flujo ordena la validación, la preparación, el progreso de descarga, la generación parcial, el resultado final y los errores. Las pantallas solo reducen esos estados a su estado visual; no conocen el orden de llamadas ni las excepciones de ML Kit.

La demo muestra y aplica límites de entrada antes de la inferencia: resumen acepta al menos 400 caracteres y hasta 4 000 tokens estimados, mientras que corrección, reescritura y el texto del chat se limitan a 1 024 tokens estimados para mantener los ejemplos breves. El chat además consulta el conteo y límite reales del modelo antes de generar. Tanto la descripción de imágenes como el chat ajustan las selecciones a un máximo de 2 048 píxeles por lado y 4 megapíxeles.

## Estructura del proyecto

```text
gemini-nano-workshop-main/
├── app/                              # Aplicación Android principal
│   └── src/main/
│       ├── java/com/sdkecuador/gemininano/
│       │   ├── data/genai/           # Clientes de ML Kit, descargas y estados de AI Core
│       │   └── ui/                   # Pantallas Compose, ViewModels, navegación y tema
│       └── res/                      # Recursos, iconos y tipografías
├── gradle/                           # Version catalog y Gradle Wrapper
├── build.gradle.kts                  # Configuración de compilación raíz
└── settings.gradle.kts               # Módulos y repositorios del proyecto
```

El código ejecutable está en el módulo `app/`.

## Gemini Nano y AICore

[Gemini Nano](https://developer.android.com/ai/gemini-nano) es un modelo fundacional de Google optimizado para ejecutarse en el dispositivo. En Android se accede a él a través de **AICore**, el servicio del sistema que administra el modelo, sus actualizaciones, funciones de seguridad y el uso del hardware local.

Esta app no empaqueta un modelo propio ni se conecta directamente al modelo: utiliza las [APIs GenAI de ML Kit](https://developers.google.com/ml-kit/genai), una capa de alto nivel sobre AICore. De esta manera puede implementar tareas ya preparadas —resumen, corrección, reescritura y descripción de imágenes— además de solicitudes personalizadas mediante la API Prompt que utiliza el chat.

Una vez descargado el componente correspondiente, las entradas, la inferencia y las respuestas se procesan localmente. Esto reduce la dependencia de conectividad y evita enviar el contenido a un servidor de generación. AICore puede compartir el modelo entre aplicaciones compatibles, lo que evita copias independientes del modelo y ayuda a conservar almacenamiento.

La disponibilidad no es universal: cambia según el modelo Gemini Nano, el fabricante, el dispositivo y la API utilizada. La app consulta ese estado antes de cada acción, pero es recomendable comprobar la [compatibilidad actual de dispositivos](https://developers.google.com/ml-kit/genai#device-support) antes de probarla. Las APIs GenAI incluidas en este proyecto se distribuyen actualmente en beta; Google también aplica cuotas por aplicación y permite inferencia solo cuando la app está en primer plano.

## Requisitos

- Android Studio con soporte para proyectos Kotlin/Compose y JDK 17.
- Un dispositivo Android físico compatible con Gemini Nano y AI Core para las tareas generativas. Consulta la [lista de dispositivos compatibles](https://developers.google.com/ml-kit/genai). El dictado Basic requiere Android 12 (API 31) o superior; esta app exige Android 14.
- Conexión a internet durante la configuración inicial y cuando el dispositivo deba descargar los modelos.
- Android 14 (API 34) o superior; la app compila contra Android SDK Platform 37.1 por el renderer A2UI. `targetSdk` sigue en 36.

## Ejecutar la aplicación

1. Abre esta carpeta, `gemini-nano-workshop-main`, en Android Studio.
2. Espera a que Gradle sincronice las dependencias.
3. Conecta un dispositivo compatible, selecciónalo como destino y ejecuta la app.
4. En la pantalla principal, revisa el estado de cada capacidad y descarga el componente requerido si la app lo solicita.

En **Resumen de texto**, el botón **Cargar ejemplo** rellena una entrada de prueba; también puedes escribir o pegar tu propio artículo.

También puedes generar un APK de depuración desde la terminal:

```bash
./gradlew assembleDebug
```

El APK resultante queda en `app/build/outputs/apk/debug/`.

## Notas de uso

- Las APIs de resumen, corrección y reescritura están configuradas actualmente para procesar contenido en inglés, aunque los textos de la interfaz estén en español.
- La disponibilidad de Gemini Nano depende del hardware, la versión de Android y de AI Core instalados en el dispositivo.
- Las funciones de IA generativa se ejecutan localmente una vez que el componente necesario está disponible en el equipo.

## Autor

**Edgardo Panchana**

- 💼 [LinkedIn](https://www.linkedin.com/in/edgardopanchana/)
- 📸 [Instagram](https://www.instagram.com/edgardopanchana/)
