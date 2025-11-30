void sp_reverb_late_complex_divide(sp_sample_t a_real, sp_sample_t a_imag, sp_sample_t b_real, sp_sample_t b_imag, sp_sample_t* out_real, sp_sample_t* out_imag) {
  sp_sample_t denom;
  denom = ((b_real * b_real) + (b_imag * b_imag));
  *out_real = (((a_real * b_real) + (a_imag * b_imag)) / denom);
  *out_imag = (((a_imag * b_real) - (a_real * b_imag)) / denom);
}
sp_sample_t sp_reverb_late_complex_magnitude(sp_sample_t value_real, sp_sample_t value_imag) {
  sp_sample_t sum;
  sum = ((value_real * value_real) + (value_imag * value_imag));
  return ((sqrt(sum)));
}
sp_sample_t sp_reverb_late_complex_argument(sp_sample_t value_real, sp_sample_t value_imag) { return ((atan2(value_imag, value_real))); }
sp_sample_t sp_reverb_late_band_gain_at(sp_reverb_late_config_t* config, sp_time_t period) {
  sp_time_t band_index;
  sp_sample_t weight;
  if (config->band_count == 0) {
    return ((0.0));
  };
  if (period <= (config->band_periods)[0]) {
    return (((config->band_gains)[0]));
  };
  if (period >= (config->band_periods)[(config->band_count - 1)]) {
    return (((config->band_gains)[(config->band_count - 1)]));
  };
  band_index = 0;
  while (((config->band_periods)[(band_index + 1)] < period)) {
    band_index = (band_index + 1);
  };
  weight = (((sp_sample_t)((period - (config->band_periods)[band_index]))) / ((sp_sample_t)(((config->band_periods)[(band_index + 1)] - (config->band_periods)[band_index]))));
  return (((config->band_gains)[band_index] + (((config->band_gains)[(band_index + 1)] - (config->band_gains)[band_index]) * weight)));
}
void sp_reverb_late_build_feedback_matrix(sp_reverb_late_config_t* config, sp_time_t period, sp_sample_t* matrix_real, sp_sample_t* matrix_imag) {
  sp_time_t line_count;
  sp_time_t row_index;
  sp_time_t column_index;
  sp_time_t index;
  sp_time_t delay_samples;
  sp_sample_t band_gain;
  sp_sample_t total_gain;
  sp_sample_t inv_period;
  sp_sample_t angle;
  sp_sample_t phase_real;
  sp_sample_t phase_imag;
  sp_sample_t mix_value;
  sp_sample_t scale;
  line_count = config->delay_count;
  band_gain = sp_reverb_late_band_gain_at(config, period);
  total_gain = (config->strength * band_gain);
  inv_period = (((sp_sample_t)(1.0)) / ((sp_sample_t)(period)));
  row_index = 0;
  while ((row_index < line_count)) {
    delay_samples = (config->delays)[row_index];
    angle = (-2.0 * ((sp_sample_t)(sp_pi)) * ((sp_sample_t)(delay_samples)) * inv_period);
    phase_real = cos(angle);
    phase_imag = sin(angle);
    column_index = 0;
    while ((column_index < line_count)) {
      index = ((sp_time_t)(((row_index * line_count) + column_index)));
      mix_value = (config->mix_row_major)[index];
      scale = (total_gain * mix_value);
      matrix_real[index] = (phase_real * scale);
      matrix_imag[index] = (phase_imag * scale);
      column_index = (column_index + 1);
    };
    row_index = (row_index + 1);
  };
}
void sp_reverb_late_build_feedback_matrix_from_polar(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* matrix_real, sp_sample_t* matrix_imag) {
  sp_time_t line_count;
  sp_time_t row_index;
  sp_time_t column_index;
  sp_time_t index;
  sp_time_t delay_samples;
  sp_sample_t total_gain;
  sp_sample_t log_radius;
  sp_sample_t radius_power;
  sp_sample_t delay_float;
  sp_sample_t phase_angle;
  sp_sample_t phase_real;
  sp_sample_t phase_imag;
  sp_sample_t mix_value;
  sp_sample_t scale;
  line_count = config->delay_count;
  total_gain = config->strength;
  log_radius = log(radius);
  row_index = 0;
  while ((row_index < line_count)) {
    delay_samples = (config->delays)[row_index];
    delay_float = ((sp_sample_t)(delay_samples));
    radius_power = exp((-1.0 * log_radius * delay_float));
    phase_angle = (-1.0 * angle * delay_float);
    phase_real = (radius_power * cos(phase_angle));
    phase_imag = (radius_power * sin(phase_angle));
    column_index = 0;
    while ((column_index < line_count)) {
      index = ((sp_time_t)(((row_index * line_count) + column_index)));
      mix_value = (config->mix_row_major)[index];
      scale = (total_gain * mix_value);
      matrix_real[index] = (phase_real * scale);
      matrix_imag[index] = (phase_imag * scale);
      column_index = (column_index + 1);
    };
    row_index = (row_index + 1);
  };
}
void sp_reverb_late_form_identity_minus_feedback(sp_time_t line_count, sp_sample_t* feedback_real, sp_sample_t* feedback_imag, sp_sample_t* a_real, sp_sample_t* a_imag) {
  sp_time_t row_index;
  sp_time_t column_index;
  sp_time_t index;
  row_index = 0;
  while ((row_index < line_count)) {
    column_index = 0;
    while ((column_index < line_count)) {
      index = ((sp_time_t)(((row_index * line_count) + column_index)));
      if (row_index == column_index) {
        a_real[index] = (((sp_sample_t)(1.0)) - feedback_real[index]);
      } else {
        a_real[index] = (((sp_sample_t)(0.0)) - feedback_real[index]);
      };
      a_imag[index] = (((sp_sample_t)(0.0)) - feedback_imag[index]);
      column_index = (column_index + 1);
    };
    row_index = (row_index + 1);
  };
}
void sp_reverb_late_lower_upper_factorization(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_time_t* pivot_index_list) {
  sp_time_t k_index;
  sp_time_t pivot_row;
  sp_time_t row_index;
  sp_time_t column_index;
  sp_time_t index_a;
  sp_time_t index_b;
  sp_sample_t pivot_abs_max;
  sp_sample_t value_abs;
  sp_sample_t temp_real;
  sp_sample_t temp_imag;
  sp_time_t temp_index;
  sp_sample_t scale_real;
  sp_sample_t scale_imag;
  sp_sample_t prod_real;
  sp_sample_t prod_imag;
  k_index = 0;
  while ((k_index < line_count)) {
    pivot_index_list[k_index] = k_index;
    k_index = (k_index + 1);
  };
  k_index = 0;
  while ((k_index < line_count)) {
    pivot_row = k_index;
    index_a = ((sp_time_t)(((k_index * line_count) + k_index)));
    pivot_abs_max = sp_reverb_late_complex_magnitude((matrix_real[index_a]), (matrix_imag[index_a]));
    row_index = (k_index + 1);
    while ((row_index < line_count)) {
      index_b = ((sp_time_t)(((row_index * line_count) + k_index)));
      value_abs = sp_reverb_late_complex_magnitude((matrix_real[index_b]), (matrix_imag[index_b]));
      if (value_abs > pivot_abs_max) {
        pivot_abs_max = value_abs;
        pivot_row = row_index;
      };
      row_index = (row_index + 1);
    };
    if (pivot_row != k_index) {
      column_index = 0;
      while ((column_index < line_count)) {
        index_a = ((sp_time_t)(((pivot_row * line_count) + column_index)));
        index_b = ((sp_time_t)(((k_index * line_count) + column_index)));
        temp_real = matrix_real[index_a];
        temp_imag = matrix_imag[index_a];
        matrix_real[index_a] = matrix_real[index_b];
        matrix_imag[index_a] = matrix_imag[index_b];
        matrix_real[index_b] = temp_real;
        matrix_imag[index_b] = temp_imag;
        column_index = (column_index + 1);
      };
      temp_index = pivot_index_list[pivot_row];
      pivot_index_list[pivot_row] = pivot_index_list[k_index];
      pivot_index_list[k_index] = temp_index;
    };
    row_index = (k_index + 1);
    while ((row_index < line_count)) {
      index_a = ((sp_time_t)(((row_index * line_count) + k_index)));
      index_b = ((sp_time_t)(((k_index * line_count) + k_index)));
      sp_reverb_late_complex_divide((matrix_real[index_a]), (matrix_imag[index_a]), (matrix_real[index_b]), (matrix_imag[index_b]), (&scale_real), (&scale_imag));
      matrix_real[index_a] = scale_real;
      matrix_imag[index_a] = scale_imag;
      column_index = (k_index + 1);
      while ((column_index < line_count)) {
        index_a = ((sp_time_t)(((row_index * line_count) + column_index)));
        index_b = ((sp_time_t)(((k_index * line_count) + column_index)));
        prod_real = ((scale_real * matrix_real[index_b]) - (scale_imag * matrix_imag[index_b]));
        prod_imag = ((scale_real * matrix_imag[index_b]) + (scale_imag * matrix_real[index_b]));
        matrix_real[index_a] = (matrix_real[index_a] - prod_real);
        matrix_imag[index_a] = (matrix_imag[index_a] - prod_imag);
        column_index = (column_index + 1);
      };
      row_index = (row_index + 1);
    };
    k_index = (k_index + 1);
  };
}
void sp_reverb_late_lower_upper_solve(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_time_t* pivot_list, sp_sample_t* right_real, sp_sample_t* right_imag, sp_sample_t* solution_real, sp_sample_t* solution_imag) {
  sp_time_t row_number;
  sp_time_t column_number;
  sp_time_t position;
  sp_sample_t element_real;
  sp_sample_t element_imag;
  sp_sample_t quotient_real;
  sp_sample_t quotient_imag;
  row_number = 0;
  while ((row_number < line_count)) {
    solution_real[row_number] = right_real[pivot_list[row_number]];
    solution_imag[row_number] = right_imag[pivot_list[row_number]];
    row_number = (row_number + 1);
  };
  row_number = 1;
  while ((row_number < line_count)) {
    column_number = 0;
    while ((column_number < row_number)) {
      position = ((sp_time_t)(((row_number * line_count) + column_number)));
      element_real = matrix_real[position];
      element_imag = matrix_imag[position];
      solution_real[row_number] = (solution_real[row_number] - ((element_real * solution_real[column_number]) - (element_imag * solution_imag[column_number])));
      solution_imag[row_number] = (solution_imag[row_number] - ((element_real * solution_imag[column_number]) + (element_imag * solution_real[column_number])));
      column_number = (column_number + 1);
    };
    row_number = (row_number + 1);
  };
  row_number = line_count;
  while ((row_number > 0)) {
    row_number = (row_number - 1);
    column_number = (row_number + 1);
    while ((column_number < line_count)) {
      position = ((sp_time_t)(((row_number * line_count) + column_number)));
      element_real = matrix_real[position];
      element_imag = matrix_imag[position];
      solution_real[row_number] = (solution_real[row_number] - ((element_real * solution_real[column_number]) - (element_imag * solution_imag[column_number])));
      solution_imag[row_number] = (solution_imag[row_number] - ((element_real * solution_imag[column_number]) + (element_imag * solution_real[column_number])));
      column_number = (column_number + 1);
    };
    position = ((sp_time_t)(((row_number * line_count) + row_number)));
    sp_reverb_late_complex_divide((solution_real[row_number]), (solution_imag[row_number]), (matrix_real[position]), (matrix_imag[position]), (&quotient_real), (&quotient_imag));
    solution_real[row_number] = quotient_real;
    solution_imag[row_number] = quotient_imag;
  };
}
void sp_reverb_late_power_iteration_dominant_eigenpair(sp_time_t line_count, sp_sample_t* matrix_real, sp_sample_t* matrix_imag, sp_sample_t* eigenvalue_real, sp_sample_t* eigenvalue_imag, sp_sample_t* eigenvector_real, sp_sample_t* eigenvector_imag, sp_time_t iteration_limit) {
  sp_sample_t* next_vector_real;
  sp_sample_t* next_vector_imag;
  sp_time_t iteration_number;
  sp_time_t row_number;
  sp_time_t column_number;
  sp_time_t position;
  sp_sample_t sum_real;
  sp_sample_t sum_imag;
  sp_sample_t norm_square;
  sp_sample_t norm_value;
  sp_sample_t inverse_norm_value;
  sp_sample_t row_sum_real;
  sp_sample_t row_sum_imag;
  next_vector_real = __builtin_alloca(((size_t)((line_count * ((sp_time_t)(sizeof(sp_sample_t)))))));
  next_vector_imag = __builtin_alloca(((size_t)((line_count * ((sp_time_t)(sizeof(sp_sample_t)))))));
  row_number = 0;
  while ((row_number < line_count)) {
    eigenvector_real[row_number] = 1.0;
    eigenvector_imag[row_number] = 0.0;
    row_number = (row_number + 1);
  };
  norm_square = 0.0;
  row_number = 0;
  while ((row_number < line_count)) {
    norm_square = (norm_square + ((eigenvector_real[row_number] * eigenvector_real[row_number]) + (eigenvector_imag[row_number] * eigenvector_imag[row_number])));
    row_number = (row_number + 1);
  };
  norm_value = sqrt(norm_square);
  inverse_norm_value = (1.0 / norm_value);
  row_number = 0;
  while ((row_number < line_count)) {
    eigenvector_real[row_number] = (eigenvector_real[row_number] * inverse_norm_value);
    eigenvector_imag[row_number] = (eigenvector_imag[row_number] * inverse_norm_value);
    row_number = (row_number + 1);
  };
  iteration_number = 0;
  while ((iteration_number < iteration_limit)) {
    row_number = 0;
    while ((row_number < line_count)) {
      sum_real = 0.0;
      sum_imag = 0.0;
      column_number = 0;
      while ((column_number < line_count)) {
        position = ((sp_time_t)(((row_number * line_count) + column_number)));
        sum_real = (sum_real + ((matrix_real[position] * eigenvector_real[column_number]) - (matrix_imag[position] * eigenvector_imag[column_number])));
        sum_imag = (sum_imag + ((matrix_real[position] * eigenvector_imag[column_number]) + (matrix_imag[position] * eigenvector_real[column_number])));
        column_number = (column_number + 1);
      };
      next_vector_real[row_number] = sum_real;
      next_vector_imag[row_number] = sum_imag;
      row_number = (row_number + 1);
    };
    norm_square = 0.0;
    row_number = 0;
    while ((row_number < line_count)) {
      norm_square = (norm_square + ((next_vector_real[row_number] * next_vector_real[row_number]) + (next_vector_imag[row_number] * next_vector_imag[row_number])));
      row_number = (row_number + 1);
    };
    norm_value = sqrt(norm_square);
    inverse_norm_value = (1.0 / norm_value);
    row_number = 0;
    while ((row_number < line_count)) {
      eigenvector_real[row_number] = (next_vector_real[row_number] * inverse_norm_value);
      eigenvector_imag[row_number] = (next_vector_imag[row_number] * inverse_norm_value);
      row_number = (row_number + 1);
    };
    iteration_number = (iteration_number + 1);
  };
  sum_real = 0.0;
  sum_imag = 0.0;
  row_number = 0;
  while ((row_number < line_count)) {
    row_sum_real = 0.0;
    row_sum_imag = 0.0;
    column_number = 0;
    while ((column_number < line_count)) {
      position = ((sp_time_t)(((row_number * line_count) + column_number)));
      row_sum_real = (row_sum_real + ((matrix_real[position] * eigenvector_real[column_number]) - (matrix_imag[position] * eigenvector_imag[column_number])));
      row_sum_imag = (row_sum_imag + ((matrix_real[position] * eigenvector_imag[column_number]) + (matrix_imag[position] * eigenvector_real[column_number])));
      column_number = (column_number + 1);
    };
    sum_real = (sum_real + ((eigenvector_real[row_number] * row_sum_real) + (eigenvector_imag[row_number] * row_sum_imag)));
    sum_imag = (sum_imag + ((eigenvector_real[row_number] * row_sum_imag) - (eigenvector_imag[row_number] * row_sum_real)));
    row_number = (row_number + 1);
  };
  *eigenvalue_real = sum_real;
  *eigenvalue_imag = sum_imag;
}
void sp_reverb_late_eigen_equation_value(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real, sp_sample_t* out_imag) {
  sp_time_t line_count;
  sp_sample_t* feedback_real;
  sp_sample_t* feedback_imag;
  sp_sample_t* matrix_real;
  sp_sample_t* matrix_imag;
  sp_time_t* pivot_list;
  uint8_t* visited_list;
  sp_time_t row_number;
  sp_time_t position;
  int32_t sign_value;
  sp_time_t cycle_begin;
  sp_time_t cycle_point;
  sp_time_t cycle_size;
  sp_sample_t determinant_real;
  sp_sample_t determinant_imag;
  sp_sample_t diagonal_real;
  sp_sample_t diagonal_imag;
  sp_sample_t prev_real;
  sp_sample_t prev_imag;
  line_count = config->delay_count;
  feedback_real = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  feedback_imag = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  matrix_real = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  matrix_imag = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  pivot_list = __builtin_alloca(((size_t)((line_count * sizeof(sp_time_t)))));
  visited_list = __builtin_alloca(((size_t)((line_count * sizeof(uint8_t)))));
  sp_reverb_late_build_feedback_matrix_from_polar(config, radius, angle, feedback_real, feedback_imag);
  sp_reverb_late_form_identity_minus_feedback(line_count, feedback_real, feedback_imag, matrix_real, matrix_imag);
  sp_reverb_late_lower_upper_factorization(line_count, matrix_real, matrix_imag, pivot_list);
  row_number = 0;
  while ((row_number < line_count)) {
    visited_list[row_number] = 0;
    row_number = (row_number + 1);
  };
  sign_value = 1;
  cycle_begin = 0;
  while ((cycle_begin < line_count)) {
    if (visited_list[cycle_begin] == 0) {
      cycle_point = cycle_begin;
      cycle_size = 0;
      while ((visited_list[cycle_point] == 0)) {
        visited_list[cycle_point] = 1;
        cycle_point = pivot_list[cycle_point];
        cycle_size = (cycle_size + 1);
      };
      if (cycle_size > 0) {
        if (((cycle_size - 1) % 2) == 1) {
          sign_value = (sign_value * -1);
        };
      };
    };
    cycle_begin = (cycle_begin + 1);
  };
  determinant_real = 1.0;
  determinant_imag = 0.0;
  row_number = 0;
  while ((row_number < line_count)) {
    position = ((sp_time_t)(((row_number * line_count) + row_number)));
    diagonal_real = matrix_real[position];
    diagonal_imag = matrix_imag[position];
    prev_real = determinant_real;
    prev_imag = determinant_imag;
    determinant_real = ((prev_real * diagonal_real) - (prev_imag * diagonal_imag));
    determinant_imag = ((prev_real * diagonal_imag) + (prev_imag * diagonal_real));
    row_number = (row_number + 1);
  };
  if (sign_value < 0) {
    determinant_real = (0.0 - determinant_real);
    determinant_imag = (0.0 - determinant_imag);
  };
  *out_real = determinant_real;
  *out_imag = determinant_imag;
}
void sp_reverb_late_eigen_equation_jacobian_finite_difference(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_dfr_dr, sp_sample_t* out_dfr_dtheta, sp_sample_t* out_dfi_dr, sp_sample_t* out_dfi_dtheta) {
  sp_sample_t radius_step;
  sp_sample_t angle_step;
  sp_sample_t radius_up;
  sp_sample_t radius_down;
  sp_sample_t angle_up;
  sp_sample_t angle_down;
  sp_sample_t real_up_radius;
  sp_sample_t imag_up_radius;
  sp_sample_t real_down_radius;
  sp_sample_t imag_down_radius;
  sp_sample_t real_up_angle;
  sp_sample_t imag_up_angle;
  sp_sample_t real_down_angle;
  sp_sample_t imag_down_angle;
  sp_sample_t span_radius;
  sp_sample_t span_angle;
  radius_step = radius;
  if (radius_step < 1.0) {
    radius_step = 1.0;
  };
  radius_step = (radius_step * 1.0e-4);
  angle_step = 1.0e-4;
  radius_up = (radius + radius_step);
  radius_down = (radius - radius_step);
  angle_up = (angle + angle_step);
  angle_down = (angle - angle_step);
  sp_reverb_late_eigen_equation_value(config, radius_up, angle, (&real_up_radius), (&imag_up_radius));
  sp_reverb_late_eigen_equation_value(config, radius_down, angle, (&real_down_radius), (&imag_down_radius));
  sp_reverb_late_eigen_equation_value(config, radius, angle_up, (&real_up_angle), (&imag_up_angle));
  sp_reverb_late_eigen_equation_value(config, radius, angle_down, (&real_down_angle), (&imag_down_angle));
  span_radius = (radius_up - radius_down);
  span_angle = (angle_up - angle_down);
  *out_dfr_dr = ((real_up_radius - real_down_radius) / span_radius);
  *out_dfi_dr = ((imag_up_radius - imag_down_radius) / span_radius);
  *out_dfr_dtheta = ((real_up_angle - real_down_angle) / span_angle);
  *out_dfi_dtheta = ((imag_up_angle - imag_down_angle) / span_angle);
}
void sp_reverb_late_newton_step_on_eigen_equation(sp_sample_t radius_current, sp_sample_t angle_current, sp_sample_t real_derivative_radius, sp_sample_t real_derivative_angle, sp_sample_t imag_derivative_radius, sp_sample_t imag_derivative_angle, sp_sample_t real_value, sp_sample_t imag_value, sp_sample_t* radius_next, sp_sample_t* angle_next) {
  sp_sample_t jacobian_determinant;
  sp_sample_t radius_update;
  sp_sample_t angle_update;
  jacobian_determinant = ((real_derivative_radius * imag_derivative_angle) - (real_derivative_angle * imag_derivative_radius));
  radius_update = (((imag_derivative_angle * real_value) - (real_derivative_angle * imag_value)) / jacobian_determinant);
  angle_update = (((imag_derivative_radius * real_value) - (real_derivative_radius * imag_value)) / jacobian_determinant);
  radius_update = (0.0 - radius_update);
  *radius_next = (radius_current + radius_update);
  *angle_next = (angle_current + angle_update);
}
void sp_reverb_late_build_state_excitation(sp_reverb_late_config_t* config, sp_reverb_position_t* position, sp_sample_t* out_real, sp_sample_t* out_imag) {
  sp_time_t delay_index;
  sp_time_t delay_count;
  sp_time_t dimension_index;
  sp_time_t dimension_count;
  sp_sample_t source_unit[sp_reverb_position_max_dimensions];
  sp_sample_t source_norm_square;
  sp_sample_t source_norm;
  sp_sample_t inverse_source_norm;
  sp_sample_t direction_norm_square;
  sp_sample_t direction_norm;
  sp_sample_t inverse_direction_norm;
  sp_sample_t cosine_value;
  sp_sample_t energy_sum;
  sp_sample_t norm_value;
  sp_sample_t inverse_norm_value;
  sp_sample_t weight;
  sp_sample_t value;
  sp_sample_t state_value;
  delay_count = config->delay_count;
  dimension_count = config->state_dimension_count;
  source_norm_square = 0.0;
  dimension_index = 0;
  while ((dimension_index < dimension_count)) {
    source_unit[dimension_index] = (position->values)[dimension_index];
    source_norm_square = (source_norm_square + (source_unit[dimension_index] * source_unit[dimension_index]));
    dimension_index = (dimension_index + 1);
  };
  if (source_norm_square > 0.0) {
    source_norm = sqrt(source_norm_square);
    inverse_source_norm = (1.0 / source_norm);
    dimension_index = 0;
    while ((dimension_index < dimension_count)) {
      source_unit[dimension_index] = (source_unit[dimension_index] * inverse_source_norm);
      dimension_index = (dimension_index + 1);
    };
  } else {
    dimension_index = 0;
    while ((dimension_index < dimension_count)) {
      source_unit[dimension_index] = 0.0;
      dimension_index = (dimension_index + 1);
    };
  };
  delay_index = 0;
  while ((delay_index < delay_count)) {
    direction_norm_square = 0.0;
    dimension_index = 0;
    while ((dimension_index < dimension_count)) {
      value = (config->state_directions)[((delay_index * dimension_count) + dimension_index)];
      direction_norm_square = (direction_norm_square + (value * value));
      dimension_index = (dimension_index + 1);
    };
    if ((direction_norm_square > 0.0) && (source_norm_square > 0.0)) {
      direction_norm = sqrt(direction_norm_square);
      inverse_direction_norm = (1.0 / direction_norm);
      cosine_value = 0.0;
      dimension_index = 0;
      while ((dimension_index < dimension_count)) {
        state_value = ((config->state_directions)[((delay_index * dimension_count) + dimension_index)] * inverse_direction_norm);
        cosine_value = (cosine_value + (state_value * source_unit[dimension_index]));
        dimension_index = (dimension_index + 1);
      };
      if (cosine_value < 0.0) {
        cosine_value = 0.0;
      };
      weight = cosine_value;
    } else {
      weight = 1.0;
    };
    out_real[delay_index] = weight;
    out_imag[delay_index] = 0.0;
    delay_index = (delay_index + 1);
  };
  energy_sum = 0.0;
  delay_index = 0;
  while ((delay_index < delay_count)) {
    energy_sum = (energy_sum + (out_real[delay_index] * out_real[delay_index]));
    delay_index = (delay_index + 1);
  };
  if (energy_sum > 0.0) {
    norm_value = sqrt(energy_sum);
    inverse_norm_value = (1.0 / norm_value);
    delay_index = 0;
    while ((delay_index < delay_count)) {
      out_real[delay_index] = (out_real[delay_index] * inverse_norm_value);
      delay_index = (delay_index + 1);
    };
  } else {
    if (delay_count > 0) {
      norm_value = sqrt(((sp_sample_t)(delay_count)));
      inverse_norm_value = (1.0 / norm_value);
      delay_index = 0;
      while ((delay_index < delay_count)) {
        out_real[delay_index] = inverse_norm_value;
        delay_index = (delay_index + 1);
      };
    };
  };
}
void sp_reverb_late_null_vector_of_shifted_matrix(sp_time_t line_count, sp_sample_t* a_real, sp_sample_t* a_imag, int use_transpose, sp_sample_t* out_real, sp_sample_t* out_imag) {
  sp_time_t reduced_count;
  sp_time_t pivot_index;
  sp_time_t row_index;
  sp_time_t column_index;
  sp_time_t reduced_row;
  sp_time_t reduced_column;
  sp_time_t index_full;
  sp_time_t index_reduced;
  sp_sample_t* reduced_real;
  sp_sample_t* reduced_imag;
  sp_sample_t* right_real;
  sp_sample_t* right_imag;
  sp_sample_t* solution_real;
  sp_sample_t* solution_imag;
  sp_time_t* pivot_list;
  sp_sample_t norm_square;
  sp_sample_t norm_value;
  sp_sample_t inverse_norm_value;
  if (line_count == 0) {
    return;
  };
  if (line_count == 1) {
    out_real[0] = 1.0;
    out_imag[0] = 0.0;
    return;
  };
  reduced_count = (line_count - 1);
  reduced_real = __builtin_alloca(((size_t)((reduced_count * reduced_count * sizeof(sp_sample_t)))));
  reduced_imag = __builtin_alloca(((size_t)((reduced_count * reduced_count * sizeof(sp_sample_t)))));
  right_real = __builtin_alloca(((size_t)((reduced_count * sizeof(sp_sample_t)))));
  right_imag = __builtin_alloca(((size_t)((reduced_count * sizeof(sp_sample_t)))));
  solution_real = __builtin_alloca(((size_t)((reduced_count * sizeof(sp_sample_t)))));
  solution_imag = __builtin_alloca(((size_t)((reduced_count * sizeof(sp_sample_t)))));
  pivot_list = __builtin_alloca(((size_t)((reduced_count * sizeof(sp_time_t)))));
  pivot_index = 0;
  reduced_row = 0;
  row_index = 0;
  while ((row_index < line_count)) {
    if (row_index != pivot_index) {
      reduced_column = 0;
      column_index = 0;
      while ((column_index < line_count)) {
        if (column_index != pivot_index) {
          if (use_transpose == 0) {
            index_full = ((sp_time_t)(((row_index * line_count) + column_index)));
          } else {
            index_full = ((sp_time_t)(((column_index * line_count) + row_index)));
          };
          index_reduced = ((sp_time_t)(((reduced_row * reduced_count) + reduced_column)));
          reduced_real[index_reduced] = a_real[index_full];
          reduced_imag[index_reduced] = a_imag[index_full];
          reduced_column = (reduced_column + 1);
        } else {
          column_index = (column_index + 1);
        };
        if (use_transpose == 0) {
          index_full = ((sp_time_t)(((row_index * line_count) + pivot_index)));
        } else {
          index_full = ((sp_time_t)(((pivot_index * line_count) + row_index)));
        };
        right_real[reduced_row] = (0.0 - a_real[index_full]);
        right_imag[reduced_row] = (0.0 - a_imag[index_full]);
        reduced_row = (reduced_row + 1);
      };
    } else {
      row_index = (row_index + 1);
    };
    sp_reverb_late_lower_upper_factorization(reduced_count, reduced_real, reduced_imag, pivot_list);
    sp_reverb_late_lower_upper_solve(reduced_count, reduced_real, reduced_imag, pivot_list, right_real, right_imag, solution_real, solution_imag);
    row_index = 0;
    while ((row_index < line_count)) {
      if (row_index == pivot_index) {
        out_real[row_index] = 1.0;
        out_imag[row_index] = 0.0;
      } else {
        if (row_index < pivot_index) {
          out_real[row_index] = solution_real[row_index];
          out_imag[row_index] = solution_imag[row_index];
        } else {
          out_real[row_index] = solution_real[(row_index - 1)];
          out_imag[row_index] = solution_imag[(row_index - 1)];
        };
      };
      row_index = (row_index + 1);
    };
    norm_square = 0.0;
    row_index = 0;
    while ((row_index < line_count)) {
      norm_square = (norm_square + (out_real[row_index] * out_real[row_index]) + (out_imag[row_index] * out_imag[row_index]));
      row_index = (row_index + 1);
    };
    if (norm_square > 0.0) {
      norm_value = sqrt(norm_square);
      inverse_norm_value = (1.0 / norm_value);
      row_index = 0;
      while ((row_index < line_count)) {
        out_real[row_index] = (out_real[row_index] * inverse_norm_value);
        out_imag[row_index] = (out_imag[row_index] * inverse_norm_value);
        row_index = (row_index + 1);
      };
    };
  };
}
sp_reverb_late_modal_set_t sp_reverb_late_modal(sp_reverb_late_config_t* config) {
  sp_reverb_late_modal_set_t modal_set;
  sp_time_t delay_line_count;
  sp_time_t mode_count;
  sp_time_t mode_index;
  size_t list_size;
  sp_sample_t radius_current;
  sp_sample_t angle_current;
  sp_sample_t value_real;
  sp_sample_t value_imag;
  sp_sample_t derivative_real_radius;
  sp_sample_t derivative_real_angle;
  sp_sample_t derivative_imag_radius;
  sp_sample_t derivative_imag_angle;
  sp_sample_t radius_next;
  sp_sample_t angle_next;
  sp_time_t iteration_index;
  sp_time_t iteration_limit;
  sp_sample_t period_float;
  sp_time_t period_integer;
  sp_sample_t decay_float;
  sp_time_t decay_integer;
  modal_set.mode_list = 0;
  modal_set.mode_count = 0;
  delay_line_count = config->delay_count;
  mode_count = delay_line_count;
  if (mode_count == 0) {
    return (modal_set);
  };
  list_size = ((size_t)(mode_count));
  modal_set.mode_list = ((sp_reverb_late_mode_t*)(malloc((list_size * sizeof(sp_reverb_late_mode_t)))));
  if (modal_set.mode_list == 0) {
    return (modal_set);
  };
  iteration_limit = 16;
  mode_index = 0;
  while ((mode_index < mode_count)) {
    radius_current = 0.9;
    angle_current = ((2.0 * sp_pi * (((sp_sample_t)(mode_index)) + 0.5)) / ((sp_sample_t)(mode_count)));
    iteration_index = 0;
    while ((iteration_index < iteration_limit)) {
      sp_reverb_late_eigen_equation_value(config, radius_current, angle_current, (&value_real), (&value_imag));
      sp_reverb_late_eigen_equation_jacobian_finite_difference(config, radius_current, angle_current, (&derivative_real_radius), (&derivative_real_angle), (&derivative_imag_radius), (&derivative_imag_angle));
      sp_reverb_late_newton_step_on_eigen_equation(radius_current, angle_current, derivative_real_radius, derivative_real_angle, derivative_imag_radius, derivative_imag_angle, value_real, value_imag, (&radius_next), (&angle_next));
      radius_current = radius_next;
      angle_current = angle_next;
      iteration_index = (iteration_index + 1);
    };
    if (radius_current <= 0.0) {
      radius_current = 1.0e-6;
    };
    if (radius_current >= 1.0) {
      radius_current = (1.0 - 1.0e-6);
    };
    if (angle_current <= 0.0) {
      angle_current = 1.0e-6;
    };
    period_float = ((2.0 * sp_pi) / angle_current);
    if (period_float < 1.0) {
      period_float = 1.0;
    };
    period_integer = ((sp_time_t)(llround(period_float)));
    decay_float = (1.0 / (-log(radius_current)));
    if (decay_float < 1.0) {
      decay_float = 1.0;
    };
    decay_integer = ((sp_time_t)(llround(decay_float)));
    ((modal_set.mode_list)[mode_index]).period = period_integer;
    ((modal_set.mode_list)[mode_index]).decay = decay_integer;
    mode_index = (mode_index + 1);
  };
  modal_set.mode_count = mode_count;
  return (modal_set);
}
void sp_reverb_late_build_state_projection(sp_reverb_late_config_t* config, sp_reverb_layout_t* layout, sp_reverb_position_t* position, sp_channel_count_t channel_index, sp_sample_t* out_real, sp_sample_t* out_imag) {
  sp_time_t delay_index;
  sp_time_t delay_count;
  sp_time_t dimension_index;
  sp_time_t dimension_count;
  sp_time_t basis_length;
  sp_sample_t channel_unit[sp_reverb_position_max_dimensions];
  sp_sample_t channel_norm_square;
  sp_sample_t channel_norm;
  sp_sample_t inverse_channel_norm;
  sp_sample_t direction_norm_square;
  sp_sample_t direction_norm;
  sp_sample_t inverse_direction_norm;
  sp_sample_t cosine_value;
  sp_sample_t weight;
  sp_sample_t energy_sum;
  sp_sample_t norm_value;
  sp_sample_t inverse_norm_value;
  sp_sample_t pan_position_normalized;
  sp_sample_t pan_gain;
  sp_sample_t channel_value;
  sp_sample_t channel_axis_value;
  sp_sample_t value;
  sp_sample_t state_value;
  delay_count = config->delay_count;
  dimension_count = config->state_dimension_count;
  basis_length = layout->basis_length;
  channel_norm_square = 0.0;
  dimension_index = 0;
  while ((dimension_index < dimension_count)) {
    if (dimension_index < basis_length) {
      channel_value = (layout->bases)[((sp_time_t)(((((sp_time_t)(channel_index)) * basis_length) + dimension_index)))];
    } else {
      channel_value = 0.0;
    };
    channel_unit[dimension_index] = channel_value;
    channel_norm_square = (channel_norm_square + (channel_value * channel_value));
    dimension_index = (dimension_index + 1);
  };
  if (channel_norm_square > 0.0) {
    channel_norm = sqrt(channel_norm_square);
    inverse_channel_norm = (1.0 / channel_norm);
    dimension_index = 0;
    while ((dimension_index < dimension_count)) {
      channel_unit[dimension_index] = (channel_unit[dimension_index] * inverse_channel_norm);
      dimension_index = (dimension_index + 1);
    };
  } else {
    dimension_index = 0;
    while ((dimension_index < dimension_count)) {
      channel_unit[dimension_index] = 0.0;
      dimension_index = (dimension_index + 1);
    };
  };
  if (dimension_count > 0) {
    pan_position_normalized = (((position->values)[0] + 1.0) * 0.5);
    if (pan_position_normalized < 0.0) {
      pan_position_normalized = 0.0;
    };
    if (pan_position_normalized > 1.0) {
      pan_position_normalized = 1.0;
    };
    if (basis_length > 0) {
      channel_axis_value = (layout->bases)[((sp_time_t)((((sp_time_t)(channel_index)) * basis_length)))];
      if (channel_axis_value < 0.0) {
        pan_gain = sqrt((1.0 - pan_position_normalized));
      } else {
        if (channel_axis_value > 0.0) {
          pan_gain = sqrt(pan_position_normalized);
        } else {
          pan_gain = 1.0;
        };
      };
    } else {
      pan_gain = 1.0;
    };
  } else {
    pan_gain = 1.0;
  };
  delay_index = 0;
  while ((delay_index < delay_count)) {
    direction_norm_square = 0.0;
    dimension_index = 0;
    while ((dimension_index < dimension_count)) {
      value = (config->state_directions)[((delay_index * dimension_count) + dimension_index)];
      direction_norm_square = (direction_norm_square + (value * value));
      dimension_index = (dimension_index + 1);
    };
    if ((direction_norm_square > 0.0) && (channel_norm_square > 0.0)) {
      direction_norm = sqrt(direction_norm_square);
      inverse_direction_norm = (1.0 / direction_norm);
      cosine_value = 0.0;
      dimension_index = 0;
      while ((dimension_index < dimension_count)) {
        state_value = ((config->state_directions)[((delay_index * dimension_count) + dimension_index)] * inverse_direction_norm);
        cosine_value = (cosine_value + (state_value * channel_unit[dimension_index]));
        dimension_index = (dimension_index + 1);
      };
      if (cosine_value < 0.0) {
        cosine_value = 0.0;
      };
      weight = cosine_value;
    } else {
      weight = 1.0;
    };
    out_real[delay_index] = weight;
    out_imag[delay_index] = 0.0;
    delay_index = (delay_index + 1);
  };
  energy_sum = 0.0;
  delay_index = 0;
  while ((delay_index < delay_count)) {
    energy_sum = (energy_sum + (out_real[delay_index] * out_real[delay_index]));
    delay_index = (delay_index + 1);
  };
  if (energy_sum > 0.0) {
    norm_value = sqrt(energy_sum);
    inverse_norm_value = (1.0 / norm_value);
    delay_index = 0;
    while ((delay_index < delay_count)) {
      out_real[delay_index] = (out_real[delay_index] * inverse_norm_value * pan_gain);
      delay_index = (delay_index + 1);
    };
  } else {
    if (delay_count > 0) {
      norm_value = sqrt(((sp_sample_t)(delay_count)));
      inverse_norm_value = (1.0 / norm_value);
      delay_index = 0;
      while ((delay_index < delay_count)) {
        out_real[delay_index] = (inverse_norm_value * pan_gain);
        delay_index = (delay_index + 1);
      };
    };
  };
}
void sp_reverb_late_right_eigenvector_at_pole(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real, sp_sample_t* out_imag) {
  sp_time_t line_count;
  sp_sample_t* feedback_real;
  sp_sample_t* feedback_imag;
  sp_sample_t* a_real;
  sp_sample_t* a_imag;
  line_count = config->delay_count;
  feedback_real = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  feedback_imag = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  a_real = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  a_imag = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  sp_reverb_late_build_feedback_matrix_from_polar(config, radius, angle, feedback_real, feedback_imag);
  sp_reverb_late_form_identity_minus_feedback(line_count, feedback_real, feedback_imag, a_real, a_imag);
  sp_reverb_late_null_vector_of_shifted_matrix(line_count, a_real, a_imag, 0, out_real, out_imag);
}
void sp_reverb_late_left_eigenvector_at_pole(sp_reverb_late_config_t* config, sp_sample_t radius, sp_sample_t angle, sp_sample_t* out_real, sp_sample_t* out_imag) {
  sp_time_t line_count;
  sp_sample_t* feedback_real;
  sp_sample_t* feedback_imag;
  sp_sample_t* a_real;
  sp_sample_t* a_imag;
  line_count = config->delay_count;
  feedback_real = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  feedback_imag = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  a_real = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  a_imag = __builtin_alloca(((size_t)((line_count * line_count * sizeof(sp_sample_t)))));
  sp_reverb_late_build_feedback_matrix_from_polar(config, radius, angle, feedback_real, feedback_imag);
  sp_reverb_late_form_identity_minus_feedback(line_count, feedback_real, feedback_imag, a_real, a_imag);
  sp_reverb_late_null_vector_of_shifted_matrix(line_count, a_real, a_imag, 1, out_real, out_imag);
}
void sp_reverb_late_modal_residues(sp_reverb_late_config_t* config, sp_reverb_late_modal_set_t* poles, sp_reverb_layout_t* layout, sp_reverb_position_t* position, sp_reverb_late_channel_modal_set_t* out_modes) {
  sp_time_t state_count;
  sp_time_t mode_count;
  sp_channel_count_t channel_count;
  size_t total_mode_count;
  sp_sample_t* state_excitation_real;
  sp_sample_t* state_excitation_imag;
  sp_sample_t* right_vector_real;
  sp_sample_t* right_vector_imag;
  sp_sample_t* left_vector_real;
  sp_sample_t* left_vector_imag;
  sp_sample_t* channel_projection_real;
  sp_sample_t* channel_projection_imag;
  sp_time_t mode_index;
  sp_channel_count_t channel_index;
  sp_sample_t period_value;
  sp_sample_t decay_value;
  sp_sample_t angle_value;
  sp_sample_t radius_value;
  sp_sample_t sum_real;
  sp_sample_t sum_imag;
  sp_sample_t s_real;
  sp_sample_t s_imag;
  sp_sample_t n_real;
  sp_sample_t n_imag;
  sp_sample_t t_real;
  sp_sample_t t_imag;
  sp_sample_t st_real;
  sp_sample_t st_imag;
  sp_sample_t alpha_real;
  sp_sample_t alpha_imag;
  sp_sample_t amplitude_value;
  sp_sample_t phase_angle;
  sp_time_t phase_samples;
  sp_time_t state_index;
  size_t channel_mode_index;
  out_modes->mode_list = 0;
  out_modes->mode_count = 0;
  out_modes->amplitude_list = 0;
  out_modes->phase_list = 0;
  out_modes->channel_count = 0;
  state_count = config->delay_count;
  mode_count = poles->mode_count;
  channel_count = layout->channel_count;
  if (state_count == 0) {
    return;
  };
  if (mode_count == 0) {
    return;
  };
  if (channel_count == 0) {
    return;
  };
  total_mode_count = (((size_t)(mode_count)) * ((size_t)(channel_count)));
  out_modes->mode_list = poles->mode_list;
  out_modes->mode_count = mode_count;
  out_modes->channel_count = channel_count;
  out_modes->amplitude_list = ((sp_sample_t*)(malloc((total_mode_count * sizeof(sp_sample_t)))));
  if (out_modes->amplitude_list == 0) {
    out_modes->mode_list = 0;
    out_modes->mode_count = 0;
    out_modes->channel_count = 0;
    return;
  };
  out_modes->phase_list = ((sp_time_t*)(malloc((total_mode_count * sizeof(sp_time_t)))));
  if (out_modes->phase_list == 0) {
    free((out_modes->amplitude_list));
    out_modes->amplitude_list = 0;
    out_modes->mode_list = 0;
    out_modes->mode_count = 0;
    out_modes->channel_count = 0;
    return;
  };
  state_excitation_real = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  state_excitation_imag = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  right_vector_real = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  right_vector_imag = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  left_vector_real = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  left_vector_imag = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  channel_projection_real = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  channel_projection_imag = ((sp_sample_t*)(__builtin_alloca((((size_t)(state_count)) * sizeof(sp_sample_t)))));
  sp_reverb_late_build_state_excitation(config, position, state_excitation_real, state_excitation_imag);
  mode_index = 0;
  while ((mode_index < mode_count)) {
    period_value = ((sp_sample_t)(((poles->mode_list)[mode_index]).period));
    decay_value = ((sp_sample_t)(((poles->mode_list)[mode_index]).decay));
    if (period_value <= 0.0) {
      period_value = 1.0;
    };
    if (decay_value <= 0.0) {
      decay_value = 1.0;
    };
    angle_value = ((2.0 * sp_pi) / period_value);
    radius_value = exp((-1.0 / decay_value));
    sp_reverb_late_right_eigenvector_at_pole(config, radius_value, angle_value, right_vector_real, right_vector_imag);
    sp_reverb_late_left_eigenvector_at_pole(config, radius_value, angle_value, left_vector_real, left_vector_imag);
    s_real = 0.0;
    s_imag = 0.0;
    state_index = 0;
    while ((state_index < state_count)) {
      sum_real = ((left_vector_real[state_index] * state_excitation_real[state_index]) - (left_vector_imag[state_index] * state_excitation_imag[state_index]));
      sum_imag = ((left_vector_real[state_index] * state_excitation_imag[state_index]) + (left_vector_imag[state_index] * state_excitation_real[state_index]));
      s_real = (s_real + sum_real);
      s_imag = (s_imag + sum_imag);
      state_index = (state_index + 1);
    };
    n_real = 0.0;
    n_imag = 0.0;
    state_index = 0;
    while ((state_index < state_count)) {
      sum_real = ((left_vector_real[state_index] * right_vector_real[state_index]) - (left_vector_imag[state_index] * right_vector_imag[state_index]));
      sum_imag = ((left_vector_real[state_index] * right_vector_imag[state_index]) + (left_vector_imag[state_index] * right_vector_real[state_index]));
      n_real = (n_real + sum_real);
      n_imag = (n_imag + sum_imag);
      state_index = (state_index + 1);
    };
    channel_index = 0;
    while ((channel_index < channel_count)) {
      sp_reverb_late_build_state_projection(config, layout, position, channel_index, channel_projection_real, channel_projection_imag);
      t_real = 0.0;
      t_imag = 0.0;
      state_index = 0;
      while ((state_index < state_count)) {
        sum_real = ((channel_projection_real[state_index] * right_vector_real[state_index]) - (channel_projection_imag[state_index] * right_vector_imag[state_index]));
        sum_imag = ((channel_projection_real[state_index] * right_vector_imag[state_index]) + (channel_projection_imag[state_index] * right_vector_real[state_index]));
        t_real = (t_real + sum_real);
        t_imag = (t_imag + sum_imag);
        state_index = (state_index + 1);
      };
      st_real = ((s_real * t_real) - (s_imag * t_imag));
      st_imag = ((s_real * t_imag) + (s_imag * t_real));
      sp_reverb_late_complex_divide(st_real, st_imag, n_real, n_imag, (&alpha_real), (&alpha_imag));
      amplitude_value = sp_reverb_late_complex_magnitude(alpha_real, alpha_imag);
      phase_angle = sp_reverb_late_complex_argument(alpha_real, alpha_imag);
      phase_samples = ((sp_time_t)(llround(((phase_angle * period_value) / (2.0 * sp_pi)))));
      channel_mode_index = ((((size_t)(channel_index)) * ((size_t)(mode_count))) + ((size_t)(mode_index)));
      (out_modes->amplitude_list)[channel_mode_index] = amplitude_value;
      (out_modes->phase_list)[channel_mode_index] = phase_samples;
      channel_index += 1;
    };
    mode_index += 1;
  };
}
