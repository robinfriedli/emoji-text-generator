package api;

import com.google.common.collect.Lists;
import core.Context;
import core.DuplicateKeywordEvent;
import core.EmojiChangingEvent;
import util.DiscordListener;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class Emoji {

    private List<Keyword> keywords;
    private String emojiValue;
    private boolean random;
    private State state;

    public Emoji(List<Keyword> keywords, String emojiValue, boolean random) {
        this.keywords = keywords;
        this.emojiValue = emojiValue;
        this.random = random;
        this.state = State.CONCEPTION;
    }

    public Emoji(List<Keyword> keywords, String emojiValue, boolean random, State state) {
        this.keywords = keywords;
        this.emojiValue = emojiValue;
        this.random = random;
        this.state = state;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public boolean hasKeyword(Keyword keyword) {
        return keywords.contains(keyword);
    }

    public boolean hasKeywordValue(String keyword) {
        List<String> keywords = this.keywords.stream().map(Keyword::getKeywordValue).collect(Collectors.toList());
        return keywords.contains(keyword);
    }

    @Nullable
    public Keyword getKeyword(String value) {
        return getKeyword(value, false);
    }

    public Keyword getKeyword(String value, boolean ignoreCase) {
        if (hasKeywordValue(value)) {
            List<Keyword> foundKeywords = keywords.stream()
                .filter(k -> ignoreCase
                    ? k.getKeywordValue().equalsIgnoreCase(value)
                    : k.getKeywordValue().equals(value))
                .collect(Collectors.toList());

            if (foundKeywords.size() == 1) {
                return foundKeywords.get(0);
            } else if (foundKeywords.size() > 1) {
                throw new IllegalStateException("Duplicate keywords on emoji " + value + ". Try " + DiscordListener.COMMAND_CLEAN);
            }
        }

        return null;
    }

    public Keyword requireKeyword(String value) {
        Keyword keyword = getKeyword(value);

        if (keyword == null) {
            throw new IllegalStateException("Keyword value " + value + " not found on emoji " + emojiValue);
        }

        return keyword;
    }

    public Keyword requireKeywordIgnoreCase(String value) {
        Keyword keyword = getKeyword(value, true);

        if (keyword == null) {
            throw new IllegalStateException("Keyword value " + value + " not found on emoji " + emojiValue);
        }

        return keyword;
    }

    public List<Keyword> getDuplicatesOf(Keyword keyword) {
        return this.keywords.stream().filter(k -> k.getKeywordValue().equals(keyword.getKeywordValue())).collect(Collectors.toList());
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    public void addKeyword(Keyword keyword) {
        this.keywords.add(keyword);
    }

    public void removeKeyword(Keyword keyword) {
        List<Keyword> foundKeywords = Lists.newArrayList();
        keywords.stream()
            .filter(k -> k.equals(keyword))
            .forEach(foundKeywords::add);

        if (foundKeywords.size() == 1) {
            keywords.remove(foundKeywords.get(0));
        } else if (foundKeywords.isEmpty()) {
            throw new IllegalStateException("No such keyword " + keyword.getKeywordValue() + " on Emoji " + this.getEmojiValue());
        } else {
            throw new IllegalStateException("Duplicate keyword " + keyword.getKeywordValue() + " on Emoji " + this.getEmojiValue()
                + ". Try " + DiscordListener.COMMAND_CLEAN);
        }
    }

    public boolean removeAll(Keyword keyword) {
        boolean performed = false;
        while (hasKeyword(keyword)) {
            performed = keywords.remove(keyword);
        }
        return performed;
    }

    public String getEmojiValue() {
        return emojiValue;
    }

    public void setEmojiValue(String emoji) {
        this.emojiValue = emoji;
    }

    public boolean isRandom() {
        return this.random;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public static List<Keyword> getAllKeywords(List<? extends Emoji> emojis) {
        List<Keyword> keywords = Lists.newArrayList();
        for (Emoji emoji : emojis) {
            keywords.addAll(emoji.getKeywords());
        }
        return keywords;
    }

    public static List<Emoji> loadFromKeyword(List<Keyword> keywords, List<Emoji> emojis) {
        List<Emoji> selectedEmojis = Lists.newArrayList();
        for (Emoji emoji : emojis) {
            List<Keyword> keywordsForEmoji = emoji.getKeywords();
            for (Keyword keyword : keywordsForEmoji) {
                if (keywords.contains(keyword)) {
                    selectedEmojis.add(emoji);
                }
            }
        }
        return selectedEmojis;
    }

    public static List<Emoji> loadFromKeyword(Keyword keyword, List<Emoji> emojis) {
        return emojis.stream()
            .filter(e -> !(e instanceof DiscordEmoji))
            .filter(e -> e.hasKeywordValue(keyword.getKeywordValue()))
            .collect(Collectors.toList());
    }

    public static Emoji loadFromValue(String value, List<Emoji> emojis) {
        List<Emoji> foundEmojis = emojis.stream().filter(e -> e.getEmojiValue().equals(value)).collect(Collectors.toList());

        if (foundEmojis.size() > 1) {
            throw new IllegalStateException("Emoji value: " + value + " not unique, try " + DiscordListener.COMMAND_CLEAN + " or fix your xml file manually");
        } else if (foundEmojis.size() == 1) {
            return foundEmojis.get(0);
        } else {
            throw new IllegalStateException("No emoji found for value: " + value + " within provided list");
        }
    }

    public enum State {

        /**
         * Emoji has been created but not yet persisted
         */
        CONCEPTION {
            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                throw new UnsupportedOperationException("Trying to call addChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call getChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public void clearChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call clearChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }
        },

        /**
         * Emoji exists in XML file and was left unchanged
         */
        CLEAN {
            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                throw new UnsupportedOperationException("Trying to call addChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call getChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public void clearChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call clearChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }
        },

        /**
         * Emoji exists in XML but has uncommitted changes
         */
        TOUCHED {
            private List<EmojiChangingEvent> changes = Lists.newArrayList();

            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                changes.add(emojiChangingEvent);
                Emoji source = emojiChangingEvent.getSource();

                if (emojiChangingEvent instanceof DuplicateKeywordEvent) {
                    context.executePersistTask(false, persistenceManager -> {
                        persistenceManager.applyDuplicateKeywordEvent((DuplicateKeywordEvent) emojiChangingEvent);
                        return null;
                    });
                } else {
                    context.executePersistTask(false, persistenceManager -> {
                            persistenceManager.applyEmojiChanges(source, emojiChangingEvent);
                            return null;
                        }
                    );
                }
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                return this.changes.stream().filter(c -> c.getSource().equals(source)).collect(Collectors.toList());
            }

            @Override
            public void clearChanges(Emoji source) {
                List<EmojiChangingEvent> changesToRemove = Lists.newArrayList();
                if (!changes.isEmpty()) {
                    for (EmojiChangingEvent event : changes) {
                        if (event.getSource().equals(source)) {
                            changesToRemove.add(event);
                        }
                    }
                }
                changes.removeAll(changesToRemove);
            }
        },

        /**
         * Emoji is being deleted but still exists in XML file
         */
        DELETION {
            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                throw new UnsupportedOperationException("Trying to call addChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call getChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public void clearChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call clearChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }
        };

        public abstract void addChanges(EmojiChangingEvent emojiChangingEvent, Context context);

        public abstract List<EmojiChangingEvent> getChanges(Emoji source);

        public abstract void clearChanges(Emoji source);

    }

}
