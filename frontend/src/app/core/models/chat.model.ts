/** One message. `sender` is always set by the server from the authenticated author. */
export interface ChatMessage {
  id: number;
  sender: string;
  recipient: string;
  content: string;
  /** ISO-8601 instant. */
  timestamp: string;
  /** Whether the current user has seen it; always true for their own messages. */
  read: boolean;
}

/** Pushed when the current user reads a conversation in another tab or on another device. */
export interface ChatRead {
  partner: string;
  upToId: number;
}

/** Someone the current user may message (a student's teachers, a teacher's students). */
export interface ChatContact {
  email: string;
  firstName: string;
  lastName: string;
  role: 'TEACHER' | 'STUDENT';
  /** Codes of the courses the two share. */
  courses: string[];
}

export type ChatUnavailableReason = 'NOT_PERMITTED' | 'QUIZ_IN_PROGRESS';

/**
 * Whether messaging is available right now. When it is not, the chat is hidden entirely; the
 * server refuses every chat call in that state regardless of what the client shows.
 */
export interface ChatStatus {
  available: boolean;
  reason: ChatUnavailableReason | null;
  /** When a quiz lock lapses on its own (ISO-8601), so the client knows when to re-check. */
  lockedUntil: string | null;
}
