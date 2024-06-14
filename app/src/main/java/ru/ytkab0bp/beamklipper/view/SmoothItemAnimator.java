/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.ytkab0bp.beamklipper.view;

import android.animation.TimeInterpolator;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.List;

import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

/**
 * This implementation of {@link RecyclerView.ItemAnimator} provides basic
 * animations on remove, add, and move events that happen to the items in
 * a RecyclerView. RecyclerView uses a DefaultItemAnimator by default.
 *
 * @see RecyclerView#setItemAnimator(RecyclerView.ItemAnimator)
 */
public class SmoothItemAnimator extends SimpleItemAnimator {
    private static final boolean DEBUG = false;

    private static TimeInterpolator sDefaultInterpolator = new PathInterpolator(0.25f, 0.1f, 0.25f, 1f);

    private ArrayList<RecyclerView.ViewHolder> mPendingRemovals = new ArrayList<>();
    private ArrayList<RecyclerView.ViewHolder> mPendingAdditions = new ArrayList<>();
    private ArrayList<MoveInfo> mPendingMoves = new ArrayList<>();
    private ArrayList<ChangeInfo> mPendingChanges = new ArrayList<>();

    ArrayList<ArrayList<RecyclerView.ViewHolder>> mAdditionsList = new ArrayList<>();
    ArrayList<ArrayList<MoveInfo>> mMovesList = new ArrayList<>();
    ArrayList<ArrayList<ChangeInfo>> mChangesList = new ArrayList<>();

    ArrayList<RecyclerView.ViewHolder> mAddAnimations = new ArrayList<>();
    ArrayList<RecyclerView.ViewHolder> mMoveAnimations = new ArrayList<>();
    ArrayList<RecyclerView.ViewHolder> mRemoveAnimations = new ArrayList<>();
    ArrayList<RecyclerView.ViewHolder> mChangeAnimations = new ArrayList<>();

    private static class MoveInfo {
        public RecyclerView.ViewHolder holder;
        public int fromX, fromY, toX, toY;

        MoveInfo(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            this.holder = holder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    private static class ChangeInfo {
        public RecyclerView.ViewHolder oldHolder, newHolder;
        public int fromX, fromY, toX, toY;
        private ChangeInfo(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder) {
            this.oldHolder = oldHolder;
            this.newHolder = newHolder;
        }

        ChangeInfo(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder,
                   int fromX, int fromY, int toX, int toY) {
            this(oldHolder, newHolder);
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        @Override
        public String toString() {
            return "ChangeInfo{"
                    + "oldHolder=" + oldHolder
                    + ", newHolder=" + newHolder
                    + ", fromX=" + fromX
                    + ", fromY=" + fromY
                    + ", toX=" + toX
                    + ", toY=" + toY
                    + '}';
        }
    }

    @Override
    public void runPendingAnimations() {
        boolean removalsPending = !mPendingRemovals.isEmpty();
        boolean movesPending = !mPendingMoves.isEmpty();
        boolean changesPending = !mPendingChanges.isEmpty();
        boolean additionsPending = !mPendingAdditions.isEmpty();
        if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
            // nothing to animate
            return;
        }
        // First, remove stuff
        for (RecyclerView.ViewHolder holder : mPendingRemovals) {
            animateRemoveImpl(holder);
        }
        mPendingRemovals.clear();
        // Next, move stuff
        if (movesPending) {
            final ArrayList<MoveInfo> moves = new ArrayList<>();
            moves.addAll(mPendingMoves);
            mMovesList.add(moves);
            mPendingMoves.clear();
            Runnable mover = new Runnable() {
                @Override
                public void run() {
                    for (MoveInfo moveInfo : moves) {
                        animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY,
                                moveInfo.toX, moveInfo.toY);
                    }
                    moves.clear();
                    mMovesList.remove(moves);
                }
            };
            if (removalsPending) {
                View view = moves.get(0).holder.itemView;
                ViewCompat.postOnAnimationDelayed(view, mover, getRemoveDuration());
            } else {
                mover.run();
            }
        }
        // Next, change stuff, to run in parallel with move animations
        if (changesPending) {
            final ArrayList<ChangeInfo> changes = new ArrayList<>();
            changes.addAll(mPendingChanges);
            mChangesList.add(changes);
            mPendingChanges.clear();
            Runnable changer = new Runnable() {
                @Override
                public void run() {
                    for (ChangeInfo change : changes) {
                        animateChangeImpl(change);
                    }
                    changes.clear();
                    mChangesList.remove(changes);
                }
            };
            if (removalsPending) {
                RecyclerView.ViewHolder holder = changes.get(0).oldHolder;
                ViewCompat.postOnAnimationDelayed(holder.itemView, changer, getRemoveDuration());
            } else {
                changer.run();
            }
        }
        // Next, add stuff
        if (additionsPending) {
            final ArrayList<RecyclerView.ViewHolder> additions = new ArrayList<>();
            additions.addAll(mPendingAdditions);
            mAdditionsList.add(additions);
            mPendingAdditions.clear();
            Runnable adder = new Runnable() {
                @Override
                public void run() {
                    for (RecyclerView.ViewHolder holder : additions) {
                        animateAddImpl(holder);
                    }
                    additions.clear();
                    mAdditionsList.remove(additions);
                }
            };
            if (removalsPending || movesPending || changesPending) {
                long removeDuration = removalsPending ? getRemoveDuration() : 0;
                long moveDuration = movesPending ? getMoveDuration() : 0;
                long changeDuration = changesPending ? getChangeDuration() : 0;
                long totalDelay = removeDuration + Math.max(moveDuration, changeDuration);
                View view = additions.get(0).itemView;
                ViewCompat.postOnAnimationDelayed(view, adder, totalDelay);
            } else {
                adder.run();
            }
        }
    }

    @Override
    public boolean animateRemove(final RecyclerView.ViewHolder holder) {
        resetAnimation(holder);
        mPendingRemovals.add(holder);
        return true;
    }

    private void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
        View view = holder.itemView;

        SpringAnimation spring = new SpringAnimation(new FloatValueHolder(0))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(1f)
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> {
                    view.setAlpha(1f - value);
                })
                .addEndListener((animation, canceled, value, velocity) -> {
                    view.setAlpha(1);
                    dispatchRemoveFinished(holder);
                    mRemoveAnimations.remove(holder);
                    dispatchFinishedWhenDone();
                });
        view.setTag(R.id.spring, spring);
        mRemoveAnimations.add(holder);
        dispatchRemoveStarting(holder);
        spring.start();
    }

    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        resetAnimation(holder);
        holder.itemView.setAlpha(0);
        mPendingAdditions.add(holder);
        return true;
    }

    void animateAddImpl(final RecyclerView.ViewHolder holder) {
        View view = holder.itemView;

        SpringAnimation spring = new SpringAnimation(new FloatValueHolder(0))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(1f)
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> {
                    view.setAlpha(value);
                })
                .addEndListener((animation, canceled, value, velocity) -> {
                    if (canceled) {
                        view.setAlpha(1);
                    }
                    dispatchAddFinished(holder);
                    mAddAnimations.remove(holder);
                    dispatchFinishedWhenDone();
                });
        view.setTag(R.id.spring, spring);
        mAddAnimations.add(holder);
        dispatchAddStarting(holder);
        spring.start();
    }

    @Override
    public boolean animateMove(final RecyclerView.ViewHolder holder, int fromX, int fromY,
                               int toX, int toY) {
        final View view = holder.itemView;
        fromX += (int) holder.itemView.getTranslationX();
        fromY += (int) holder.itemView.getTranslationY();
        resetAnimation(holder);
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }
        if (deltaX != 0) {
            view.setTranslationX(-deltaX);
        }
        if (deltaY != 0) {
            view.setTranslationY(-deltaY);
        }
        mPendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
        return true;
    }

    void animateMoveImpl(final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        View view = holder.itemView;
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        float fX = view.getTranslationX(), fY = view.getTranslationY();
        SpringAnimation spring = new SpringAnimation(new FloatValueHolder(0))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(1f)
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> {
                    view.setTranslationX(ViewUtils.lerp(fX, 0, value));
                    view.setTranslationY(ViewUtils.lerp(fY, 0, value));
                })
                .addEndListener((animation, canceled, value, velocity) -> {
                    if (canceled) {
                        if (deltaX != 0) {
                            view.setTranslationX(0);
                        }
                        if (deltaY != 0) {
                            view.setTranslationY(0);
                        }
                    }
                    dispatchMoveFinished(holder);
                    mMoveAnimations.remove(holder);
                    dispatchFinishedWhenDone();
                });
        view.setTag(R.id.spring, spring);
        mMoveAnimations.add(holder);
        dispatchMoveStarting(holder);
        spring.start();
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder,
                                 int fromX, int fromY, int toX, int toY) {
        if (oldHolder == newHolder) {
            // Don't know how to run change animations when the same view holder is re-used.
            // run a move animation to handle position changes.
            return animateMove(oldHolder, fromX, fromY, toX, toY);
        }
        final float prevTranslationX = oldHolder.itemView.getTranslationX();
        final float prevTranslationY = oldHolder.itemView.getTranslationY();
        final float prevAlpha = oldHolder.itemView.getAlpha();
        resetAnimation(oldHolder);
        int deltaX = (int) (toX - fromX - prevTranslationX);
        int deltaY = (int) (toY - fromY - prevTranslationY);
        // recover prev translation state after ending animation
        oldHolder.itemView.setTranslationX(prevTranslationX);
        oldHolder.itemView.setTranslationY(prevTranslationY);
        oldHolder.itemView.setAlpha(prevAlpha);
        if (newHolder != null) {
            // carry over translation values
            resetAnimation(newHolder);
            newHolder.itemView.setTranslationX(-deltaX);
            newHolder.itemView.setTranslationY(-deltaY);
            newHolder.itemView.setAlpha(0);
        }
        mPendingChanges.add(new ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY));
        return true;
    }

    void animateChangeImpl(final ChangeInfo changeInfo) {
        final RecyclerView.ViewHolder holder = changeInfo.oldHolder;
        final View view = holder == null ? null : holder.itemView;
        final RecyclerView.ViewHolder newHolder = changeInfo.newHolder;
        final View newView = newHolder != null ? newHolder.itemView : null;
        if (view != null) {
            float fX = view.getTranslationX(), fY = view.getTranslationY();
            float tX = changeInfo.toX - changeInfo.fromX, tY = changeInfo.toY - changeInfo.fromY;
            SpringAnimation spring = new SpringAnimation(new FloatValueHolder(0))
                    .setMinimumVisibleChange(1 / 256f)
                    .setSpring(new SpringForce(1f)
                            .setStiffness(1000f)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                    .addUpdateListener((animation, value, velocity) -> {
                        view.setTranslationX(ViewUtils.lerp(fX, tX, value));
                        view.setTranslationY(ViewUtils.lerp(fY, tY, value));
                        view.setAlpha(1f - value);
                    })
                    .addEndListener((animation, canceled, value, velocity) -> {
                        view.setAlpha(1);
                        view.setTranslationX(0);
                        view.setTranslationY(0);
                        dispatchChangeFinished(changeInfo.oldHolder, true);
                        mChangeAnimations.remove(changeInfo.oldHolder);
                        dispatchFinishedWhenDone();
                    });
            view.setTag(R.id.spring, spring);
            mChangeAnimations.add(changeInfo.oldHolder);
            dispatchChangeStarting(changeInfo.oldHolder, true);
            spring.start();
        }
        if (newView != null) {
            float fX = newView.getTranslationX(), fY = newView.getTranslationY();
            SpringAnimation spring = new SpringAnimation(new FloatValueHolder(0))
                    .setMinimumVisibleChange(1 / 256f)
                    .setSpring(new SpringForce(1f)
                            .setStiffness(1000f)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                    .addUpdateListener((animation, value, velocity) -> {
                        newView.setTranslationX(fX * (1f - value));
                        newView.setTranslationY(fY * (1f - value));
                    })
                    .addEndListener((animation, canceled, value, velocity) -> {
                        newView.setAlpha(1);
                        newView.setTranslationX(0);
                        newView.setTranslationY(0);
                        dispatchChangeFinished(changeInfo.newHolder, false);
                        mChangeAnimations.remove(changeInfo.newHolder);
                        dispatchFinishedWhenDone();
                    });
            newView.setTag(R.id.spring, spring);
            mChangeAnimations.add(changeInfo.newHolder);
            dispatchChangeStarting(changeInfo.newHolder, false);
            spring.start();
        }
    }

    private void endChangeAnimation(List<ChangeInfo> infoList, RecyclerView.ViewHolder item) {
        for (int i = infoList.size() - 1; i >= 0; i--) {
            ChangeInfo changeInfo = infoList.get(i);
            if (endChangeAnimationIfNecessary(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                    infoList.remove(changeInfo);
                }
            }
        }
    }

    private void endChangeAnimationIfNecessary(ChangeInfo changeInfo) {
        if (changeInfo.oldHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder);
        }
        if (changeInfo.newHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder);
        }
    }
    private boolean endChangeAnimationIfNecessary(ChangeInfo changeInfo, RecyclerView.ViewHolder item) {
        boolean oldItem = false;
        if (changeInfo.newHolder == item) {
            changeInfo.newHolder = null;
        } else if (changeInfo.oldHolder == item) {
            changeInfo.oldHolder = null;
            oldItem = true;
        } else {
            return false;
        }
        item.itemView.setAlpha(1);
        item.itemView.setTranslationX(0);
        item.itemView.setTranslationY(0);
        dispatchChangeFinished(item, oldItem);
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        final View view = item.itemView;
        // this will trigger end callback which should set properties to their target values.
        SpringAnimation spring = (SpringAnimation) view.getTag(R.id.spring);
        if (spring != null) {
            spring.cancel();
        }

        // TODO if some other animations are chained to end, how do we cancel them as well?
        for (int i = mPendingMoves.size() - 1; i >= 0; i--) {
            MoveInfo moveInfo = mPendingMoves.get(i);
            if (moveInfo.holder == item) {
                view.setTranslationY(0);
                view.setTranslationX(0);
                dispatchMoveFinished(item);
                mPendingMoves.remove(i);
            }
        }
        endChangeAnimation(mPendingChanges, item);
        if (mPendingRemovals.remove(item)) {
            view.setAlpha(1);
            dispatchRemoveFinished(item);
        }
        if (mPendingAdditions.remove(item)) {
            view.setAlpha(1);
            dispatchAddFinished(item);
        }

        for (int i = mChangesList.size() - 1; i >= 0; i--) {
            ArrayList<ChangeInfo> changes = mChangesList.get(i);
            endChangeAnimation(changes, item);
            if (changes.isEmpty()) {
                mChangesList.remove(i);
            }
        }
        for (int i = mMovesList.size() - 1; i >= 0; i--) {
            ArrayList<MoveInfo> moves = mMovesList.get(i);
            for (int j = moves.size() - 1; j >= 0; j--) {
                MoveInfo moveInfo = moves.get(j);
                if (moveInfo.holder == item) {
                    view.setTranslationY(0);
                    view.setTranslationX(0);
                    dispatchMoveFinished(item);
                    moves.remove(j);
                    if (moves.isEmpty()) {
                        mMovesList.remove(i);
                    }
                    break;
                }
            }
        }
        for (int i = mAdditionsList.size() - 1; i >= 0; i--) {
            ArrayList<RecyclerView.ViewHolder> additions = mAdditionsList.get(i);
            if (additions.remove(item)) {
                view.setAlpha(1);
                dispatchAddFinished(item);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(i);
                }
            }
        }

        // animations should be ended by the cancel above.
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mRemoveAnimations.remove(item) && DEBUG) {
            throw new IllegalStateException("after animation is cancelled, item should not be in "
                    + "mRemoveAnimations list");
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mAddAnimations.remove(item) && DEBUG) {
            throw new IllegalStateException("after animation is cancelled, item should not be in "
                    + "mAddAnimations list");
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mChangeAnimations.remove(item) && DEBUG) {
            throw new IllegalStateException("after animation is cancelled, item should not be in "
                    + "mChangeAnimations list");
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mMoveAnimations.remove(item) && DEBUG) {
            throw new IllegalStateException("after animation is cancelled, item should not be in "
                    + "mMoveAnimations list");
        }
        dispatchFinishedWhenDone();
    }

    private void resetAnimation(RecyclerView.ViewHolder holder) {
        endAnimation(holder);
    }

    @Override
    public boolean isRunning() {
        return (!mPendingAdditions.isEmpty()
                || !mPendingChanges.isEmpty()
                || !mPendingMoves.isEmpty()
                || !mPendingRemovals.isEmpty()
                || !mMoveAnimations.isEmpty()
                || !mRemoveAnimations.isEmpty()
                || !mAddAnimations.isEmpty()
                || !mChangeAnimations.isEmpty()
                || !mMovesList.isEmpty()
                || !mAdditionsList.isEmpty()
                || !mChangesList.isEmpty());
    }

    /**
     * Check the state of currently pending and running animations. If there are none
     * pending/running, call {@link #dispatchAnimationsFinished()} to notify any
     * listeners.
     */
    void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public void endAnimations() {
        int count = mPendingMoves.size();
        for (int i = count - 1; i >= 0; i--) {
            MoveInfo item = mPendingMoves.get(i);
            View view = item.holder.itemView;
            view.setTranslationY(0);
            view.setTranslationX(0);
            dispatchMoveFinished(item.holder);
            mPendingMoves.remove(i);
        }
        count = mPendingRemovals.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mPendingRemovals.get(i);
            dispatchRemoveFinished(item);
            mPendingRemovals.remove(i);
        }
        count = mPendingAdditions.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mPendingAdditions.get(i);
            item.itemView.setAlpha(1);
            dispatchAddFinished(item);
            mPendingAdditions.remove(i);
        }
        count = mPendingChanges.size();
        for (int i = count - 1; i >= 0; i--) {
            endChangeAnimationIfNecessary(mPendingChanges.get(i));
        }
        mPendingChanges.clear();
        if (!isRunning()) {
            return;
        }

        int listCount = mMovesList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            ArrayList<MoveInfo> moves = mMovesList.get(i);
            count = moves.size();
            for (int j = count - 1; j >= 0; j--) {
                MoveInfo moveInfo = moves.get(j);
                RecyclerView.ViewHolder item = moveInfo.holder;
                View view = item.itemView;
                view.setTranslationY(0);
                view.setTranslationX(0);
                dispatchMoveFinished(moveInfo.holder);
                moves.remove(j);
                if (moves.isEmpty()) {
                    mMovesList.remove(moves);
                }
            }
        }
        listCount = mAdditionsList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            ArrayList<RecyclerView.ViewHolder> additions = mAdditionsList.get(i);
            count = additions.size();
            for (int j = count - 1; j >= 0; j--) {
                RecyclerView.ViewHolder item = additions.get(j);
                View view = item.itemView;
                view.setAlpha(1);
                dispatchAddFinished(item);
                additions.remove(j);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(additions);
                }
            }
        }
        listCount = mChangesList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            ArrayList<ChangeInfo> changes = mChangesList.get(i);
            count = changes.size();
            for (int j = count - 1; j >= 0; j--) {
                endChangeAnimationIfNecessary(changes.get(j));
                if (changes.isEmpty()) {
                    mChangesList.remove(changes);
                }
            }
        }

        cancelAll(mRemoveAnimations);
        cancelAll(mMoveAnimations);
        cancelAll(mAddAnimations);
        cancelAll(mChangeAnimations);

        dispatchAnimationsFinished();
    }

    void cancelAll(List<RecyclerView.ViewHolder> viewHolders) {
        for (int i = viewHolders.size() - 1; i >= 0; i--) {
            SpringAnimation spring = (SpringAnimation) viewHolders.get(i).itemView.getTag(R.id.spring);
            if (spring != null) {
                spring.cancel();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the payload list is not empty, DefaultItemAnimator returns <code>true</code>.
     * When this is the case:
     * <ul>
     * <li>If you override {@link #animateChange(RecyclerView.ViewHolder, RecyclerView.ViewHolder, int, int, int, int)}, both
     * ViewHolder arguments will be the same instance.
     * </li>
     * <li>
     * If you are not overriding {@link #animateChange(RecyclerView.ViewHolder, RecyclerView.ViewHolder, int, int, int, int)},
     * then DefaultItemAnimator will call {@link #animateMove(RecyclerView.ViewHolder, int, int, int, int)} and
     * run a move animation instead.
     * </li>
     * </ul>
     */
    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder,
                                             @NonNull List<Object> payloads) {
        return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads);
    }
}
