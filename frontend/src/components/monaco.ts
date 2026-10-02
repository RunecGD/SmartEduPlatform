import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor/editor/editor.api';
import EditorWorker from 'monaco-editor/editor/editor.worker?worker';
import 'monaco-editor/languages/definitions/python/register';
import 'monaco-editor/languages/definitions/java/register';
import 'monaco-editor/languages/definitions/cpp/register';
// Syntax highlighting is not a declaration of backend-supported languages.
self.MonacoEnvironment = { getWorker: () => new EditorWorker() };
loader.config({monaco});
