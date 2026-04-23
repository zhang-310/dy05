import { useReducer, useCallback } from 'react'
import type { SvDramaEpisode, SvDramaCharacter } from '@/api/shortvideo'

// ─── Drama 表单状态 ───────────────────────────────────────────────────────
export interface DramaFormState {
  title: string
  desc: string
  genre: string
  episodes: number
}

const initialDramaForm: DramaFormState = { title: '', desc: '', genre: '都市', episodes: 10 }

type DramaFormAction =
  | { type: 'SET'; payload: Partial<DramaFormState> }
  | { type: 'RESET' }
  | { type: 'LOAD'; payload: DramaFormState }

function dramaFormReducer(state: DramaFormState, action: DramaFormAction): DramaFormState {
  switch (action.type) {
    case 'SET':
      return { ...state, ...action.payload }
    case 'RESET':
      return initialDramaForm
    case 'LOAD':
      return action.payload
    default:
      return state
  }
}

export function useDramaForm() {
  const [state, dispatch] = useReducer(dramaFormReducer, initialDramaForm)
  return {
    ...state,
    set: useCallback((p: Partial<DramaFormState>) => dispatch({ type: 'SET', payload: p }), []),
    reset: useCallback(() => dispatch({ type: 'RESET' }), []),
    load: useCallback((p: DramaFormState) => dispatch({ type: 'LOAD', payload: p }), []),
  }
}

// ─── Episode 表单状态 ─────────────────────────────────────────────────────
export interface EpisodeFormState {
  title: string
  synopsis: string
  cliffhanger: string
  editing: SvDramaEpisode | null
}

const initialEpisodeForm: EpisodeFormState = { title: '', synopsis: '', cliffhanger: '', editing: null }

type EpisodeFormAction =
  | { type: 'SET'; payload: Partial<Pick<EpisodeFormState, 'title' | 'synopsis' | 'cliffhanger'>> }
  | { type: 'RESET' }
  | { type: 'LOAD_EDIT'; payload: SvDramaEpisode }
  | { type: 'CLEAR_EDIT' }

function episodeFormReducer(state: EpisodeFormState, action: EpisodeFormAction): EpisodeFormState {
  switch (action.type) {
    case 'SET':
      return { ...state, ...action.payload }
    case 'RESET':
      return initialEpisodeForm
    case 'LOAD_EDIT':
      return {
        title: action.payload.title ?? '',
        synopsis: action.payload.synopsis ?? '',
        cliffhanger: action.payload.cliffhanger ?? '',
        editing: action.payload,
      }
    case 'CLEAR_EDIT':
      return { ...state, editing: null }
    default:
      return state
  }
}

export function useEpisodeForm() {
  const [state, dispatch] = useReducer(episodeFormReducer, initialEpisodeForm)
  return {
    ...state,
    set: useCallback((p: Partial<Pick<EpisodeFormState, 'title' | 'synopsis' | 'cliffhanger'>>) =>
      dispatch({ type: 'SET', payload: p }), []),
    reset: useCallback(() => dispatch({ type: 'RESET' }), []),
    loadEdit: useCallback((ep: SvDramaEpisode) => dispatch({ type: 'LOAD_EDIT', payload: ep }), []),
    clearEdit: useCallback(() => dispatch({ type: 'CLEAR_EDIT' }), []),
  }
}

// ─── Character 表单状态 ───────────────────────────────────────────────────
export interface CharacterFormState {
  name: string
  desc: string
  editing: SvDramaCharacter | null
}

const initialCharacterForm: CharacterFormState = { name: '', desc: '', editing: null }

type CharacterFormAction =
  | { type: 'SET'; payload: Partial<Pick<CharacterFormState, 'name' | 'desc'>> }
  | { type: 'RESET' }
  | { type: 'LOAD_EDIT'; payload: SvDramaCharacter }
  | { type: 'CLEAR_EDIT' }

function characterFormReducer(state: CharacterFormState, action: CharacterFormAction): CharacterFormState {
  switch (action.type) {
    case 'SET':
      return { ...state, ...action.payload }
    case 'RESET':
      return initialCharacterForm
    case 'LOAD_EDIT':
      return { name: action.payload.characterName ?? '', desc: action.payload.description ?? '', editing: action.payload }
    case 'CLEAR_EDIT':
      return { ...state, editing: null }
    default:
      return state
  }
}

export function useCharacterForm() {
  const [state, dispatch] = useReducer(characterFormReducer, initialCharacterForm)
  return {
    ...state,
    set: useCallback((p: Partial<Pick<CharacterFormState, 'name' | 'desc'>>) =>
      dispatch({ type: 'SET', payload: p }), []),
    reset: useCallback(() => dispatch({ type: 'RESET' }), []),
    loadEdit: useCallback((c: SvDramaCharacter) => dispatch({ type: 'LOAD_EDIT', payload: c }), []),
    clearEdit: useCallback(() => dispatch({ type: 'CLEAR_EDIT' }), []),
  }
}

// ─── UI 状态（弹窗、loading、确认等）───────────────────────────────────────
export interface DramaEditorUIState {
  createOpen: boolean
  episodeOpen: boolean
  characterOpen: boolean
  editDramaOpen: boolean
  editEpisodeOpen: boolean
  editCharOpen: boolean
  deleteConfirmOpen: boolean
  deleteCharConfirmOpen: boolean
  deleteEpConfirmOpen: boolean
  expandedEpisodeId: number | null
  loading: boolean
  error: string
  generatingScript: boolean
  producingEpisodeId: number | null
  deletingCharId: number | null
  deletingEpId: number | null
  scriptResult: string | null
  scriptTheme: string
  scriptStyle: string
  /** D-4：''=不传用后端的 app.shortvideo.drama.cliffhanger-carryover；否则 strong|normal|off */
  scriptCliffhangerCarryover: '' | 'normal' | 'strong' | 'off'
  applySuccess: number | null
  charToDelete: SvDramaCharacter | null
  epToDelete: SvDramaEpisode | null
}

const initialUI: DramaEditorUIState = {
  createOpen: false,
  episodeOpen: false,
  characterOpen: false,
  editDramaOpen: false,
  editEpisodeOpen: false,
  editCharOpen: false,
  deleteConfirmOpen: false,
  deleteCharConfirmOpen: false,
  deleteEpConfirmOpen: false,
  expandedEpisodeId: null,
  loading: false,
  error: '',
  generatingScript: false,
  producingEpisodeId: null,
  deletingCharId: null,
  deletingEpId: null,
  scriptResult: null,
  scriptTheme: '',
  scriptStyle: '',
  scriptCliffhangerCarryover: '',
  applySuccess: null,
  charToDelete: null,
  epToDelete: null,
}

type UIAction =
  | { type: 'SET'; payload: Partial<DramaEditorUIState> }
  | { type: 'OPEN_CREATE' }
  | { type: 'CLOSE_CREATE' }
  | { type: 'OPEN_EPISODE' }
  | { type: 'CLOSE_EPISODE' }
  | { type: 'OPEN_CHARACTER' }
  | { type: 'CLOSE_CHARACTER' }
  | { type: 'OPEN_EDIT_DRAMA' }
  | { type: 'CLOSE_EDIT_DRAMA' }
  | { type: 'OPEN_EDIT_EPISODE' }
  | { type: 'CLOSE_EDIT_EPISODE' }
  | { type: 'OPEN_EDIT_CHAR' }
  | { type: 'CLOSE_EDIT_CHAR' }
  | { type: 'OPEN_DELETE_CHAR'; payload: SvDramaCharacter }
  | { type: 'CLOSE_DELETE_CHAR' }
  | { type: 'OPEN_DELETE_EP'; payload: SvDramaEpisode }
  | { type: 'CLOSE_DELETE_EP' }
  | { type: 'OPEN_DELETE_DRAMA' }
  | { type: 'CLOSE_DELETE_DRAMA' }
  | { type: 'SET_ERROR'; payload: string }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_SCRIPT_RESULT'; payload: string | null }
  | { type: 'SET_APPLY_SUCCESS'; payload: number | null }

function uiReducer(state: DramaEditorUIState, action: UIAction): DramaEditorUIState {
  switch (action.type) {
    case 'SET':
      return { ...state, ...action.payload }
    case 'OPEN_CREATE': return { ...state, createOpen: true }
    case 'CLOSE_CREATE': return { ...state, createOpen: false }
    case 'OPEN_EPISODE': return { ...state, episodeOpen: true }
    case 'CLOSE_EPISODE': return { ...state, episodeOpen: false }
    case 'OPEN_CHARACTER': return { ...state, characterOpen: true }
    case 'CLOSE_CHARACTER': return { ...state, characterOpen: false }
    case 'OPEN_EDIT_DRAMA': return { ...state, editDramaOpen: true }
    case 'CLOSE_EDIT_DRAMA': return { ...state, editDramaOpen: false }
    case 'OPEN_EDIT_EPISODE': return { ...state, editEpisodeOpen: true }
    case 'CLOSE_EDIT_EPISODE': return { ...state, editEpisodeOpen: false }
    case 'OPEN_EDIT_CHAR': return { ...state, editCharOpen: true }
    case 'CLOSE_EDIT_CHAR': return { ...state, editCharOpen: false }
    case 'OPEN_DELETE_CHAR': return { ...state, deleteCharConfirmOpen: true, charToDelete: action.payload }
    case 'CLOSE_DELETE_CHAR': return { ...state, deleteCharConfirmOpen: false, charToDelete: null }
    case 'OPEN_DELETE_EP': return { ...state, deleteEpConfirmOpen: true, epToDelete: action.payload }
    case 'CLOSE_DELETE_EP': return { ...state, deleteEpConfirmOpen: false, epToDelete: null, expandedEpisodeId: null }
    case 'OPEN_DELETE_DRAMA': return { ...state, deleteConfirmOpen: true }
    case 'CLOSE_DELETE_DRAMA': return { ...state, deleteConfirmOpen: false }
    case 'SET_ERROR': return { ...state, error: action.payload }
    case 'SET_LOADING': return { ...state, loading: action.payload }
    case 'SET_SCRIPT_RESULT': return { ...state, scriptResult: action.payload }
    case 'SET_APPLY_SUCCESS': return { ...state, applySuccess: action.payload }
    default:
      return state
  }
}

export function useDramaEditorUI() {
  const [state, dispatch] = useReducer(uiReducer, initialUI)
  return {
    ...state,
    set: useCallback((p: Partial<DramaEditorUIState>) => dispatch({ type: 'SET', payload: p }), []),
    openCreate: useCallback(() => dispatch({ type: 'OPEN_CREATE' }), []),
    closeCreate: useCallback(() => dispatch({ type: 'CLOSE_CREATE' }), []),
    openEpisode: useCallback(() => dispatch({ type: 'OPEN_EPISODE' }), []),
    closeEpisode: useCallback(() => dispatch({ type: 'CLOSE_EPISODE' }), []),
    openCharacter: useCallback(() => dispatch({ type: 'OPEN_CHARACTER' }), []),
    closeCharacter: useCallback(() => dispatch({ type: 'CLOSE_CHARACTER' }), []),
    openEditDrama: useCallback(() => dispatch({ type: 'OPEN_EDIT_DRAMA' }), []),
    closeEditDrama: useCallback(() => dispatch({ type: 'CLOSE_EDIT_DRAMA' }), []),
    openEditEpisode: useCallback(() => dispatch({ type: 'OPEN_EDIT_EPISODE' }), []),
    closeEditEpisode: useCallback(() => dispatch({ type: 'CLOSE_EDIT_EPISODE' }), []),
    openEditChar: useCallback(() => dispatch({ type: 'OPEN_EDIT_CHAR' }), []),
    closeEditChar: useCallback(() => dispatch({ type: 'CLOSE_EDIT_CHAR' }), []),
    openDeleteChar: useCallback((c: SvDramaCharacter) => dispatch({ type: 'OPEN_DELETE_CHAR', payload: c }), []),
    closeDeleteChar: useCallback(() => dispatch({ type: 'CLOSE_DELETE_CHAR' }), []),
    openDeleteEp: useCallback((ep: SvDramaEpisode) => dispatch({ type: 'OPEN_DELETE_EP', payload: ep }), []),
    closeDeleteEp: useCallback(() => dispatch({ type: 'CLOSE_DELETE_EP' }), []),
    openDeleteDrama: useCallback(() => dispatch({ type: 'OPEN_DELETE_DRAMA' }), []),
    closeDeleteDrama: useCallback(() => dispatch({ type: 'CLOSE_DELETE_DRAMA' }), []),
    setError: useCallback((e: string) => dispatch({ type: 'SET_ERROR', payload: e }), []),
    setLoading: useCallback((v: boolean) => dispatch({ type: 'SET_LOADING', payload: v }), []),
    setScriptResult: useCallback((v: string | null) => dispatch({ type: 'SET_SCRIPT_RESULT', payload: v }), []),
    setApplySuccess: useCallback((v: number | null) => dispatch({ type: 'SET_APPLY_SUCCESS', payload: v }), []),
  }
}
